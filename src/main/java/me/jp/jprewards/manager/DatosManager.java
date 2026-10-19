package me.jp.jprewards.manager;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.DatosJugador;
import me.jp.jprewards.storage.Almacen;
import me.jp.jprewards.storage.AlmacenSql;
import me.jp.jprewards.storage.AlmacenYaml;
import me.jp.jprewards.storage.ErrorAlmacen;
import me.jp.jprewards.storage.TipoAlmacen;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Datos de los jugadores: cache en memoria + guardado en segundo plano.
 *
 * Como funciona:
 *   - Al pedir los datos de un jugador se leen del almacen y se recuerdan.
 *   - Guardar NO escribe: serializa los datos en el hilo principal (rapido y
 *     sin compartir objetos mutables) y deja el texto en una cola; el hilo
 *     de guardado los escribe despues, uno a uno y en orden.
 *   - Al salir un jugador, al apagar el servidor y en el autoguardado se
 *     vacia la cola, asi que no se pierde nada.
 *
 * Si la base de datos no responde, el plugin sigue funcionando con YAML y
 * avisa por consola en vez de dejar a los jugadores sin recompensas.
 */
public final class DatosManager {

    private final JPRewards plugin;
    private final Map<UUID, DatosJugador> cache = new ConcurrentHashMap<UUID, DatosJugador>();
    private final Map<UUID, String[]> cola = new LinkedHashMap<UUID, String[]>();
    /** Version conocida y ultima comprobacion (solo si el almacen la soporta). */
    private final Map<UUID, Long> versiones = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> comprobado = new ConcurrentHashMap<UUID, Long>();
    private static final long COMPROBAR_CADA_MS = 2500L;
    private final AtomicBoolean vaciando = new AtomicBoolean(false);

    private Almacen almacen;
    private TipoAlmacen tipoReal = TipoAlmacen.YAML;
    private BukkitTask autoguardado;

    public DatosManager(JPRewards plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Arranque
    // ------------------------------------------------------------------

    /** Prepara el almacen configurado y el autoguardado. */
    public void iniciar() {
        almacen = crearAlmacen();
        try {
            almacen.iniciar();
        } catch (Throwable error) {
            plugin.getLogger().severe("Could not prepare the storage (" + error.getMessage() + ").");
            if (tipoReal == TipoAlmacen.MYSQL) {
                plugin.getLogger().severe("Falling back to YAML: check almacenamiento.mysql in config.yml, restart and use"
                        + " '/rewards migrate' to copy the data into the database.");
                tipoReal = TipoAlmacen.YAML;
                almacen = new AlmacenYaml(plugin, plugin.ajustes().almacenCarpeta(),
                        "unico".equals(plugin.ajustes().almacenModo()));
                try {
                    almacen.iniciar();
                } catch (Throwable error2) {
                    plugin.getLogger().severe("Could not use YAML either: " + error2.getMessage());
                }
            }
        }
        iniciarAutoguardado();
    }

    private Almacen crearAlmacen() {
        if (plugin.ajustes().almacenTipo() == TipoAlmacen.MYSQL) {
            tipoReal = TipoAlmacen.MYSQL;
            return new AlmacenSql(plugin, plugin.ajustes().sql());
        }
        tipoReal = TipoAlmacen.YAML;
        return new AlmacenYaml(plugin, plugin.ajustes().almacenCarpeta(), "unico".equals(plugin.ajustes().almacenModo()));
    }

    private void iniciarAutoguardado() {
        cancelarAutoguardado();
        int segundos = plugin.ajustes().autoguardadoSegundos();
        if (segundos <= 0) {
            return;
        }
        long periodo = segundos * 20L;
        autoguardado = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                for (DatosJugador datos : new ArrayList<DatosJugador>(cache.values())) {
                    guardar(datos);
                }
                volcar();
            }
        }, periodo, periodo);
    }

    private void cancelarAutoguardado() {
        if (autoguardado != null) {
            autoguardado.cancel();
            autoguardado = null;
        }
    }

    /** Reinicia el autoguardado (tras /rewards reload). */
    public void reiniciarAutoguardado() {
        iniciarAutoguardado();
    }

    // ------------------------------------------------------------------
    // Lectura
    // ------------------------------------------------------------------

    public DatosJugador obtener(UUID uuid) {
        return obtener(uuid, null);
    }

    /** Datos de un jugador (de memoria o del almacen) con su ultimo nombre conocido. */
    public DatosJugador obtener(UUID uuid, String nombre) {
        DatosJugador datos = cache.get(uuid);
        if (datos == null) {
            try {
                datos = almacen.cargar(uuid, nombre);
            } catch (ErrorAlmacen error) {
                plugin.getLogger().warning("Could not read the data of " + uuid + ": " + error.getMessage());
                datos = new DatosJugador(uuid);
            }
            if (nombre != null && !nombre.isEmpty()) {
                datos.nombre(nombre);
            }
            cache.put(uuid, datos);
            recordarVersion(uuid);
        } else if (nombre != null && !nombre.isEmpty()) {
            datos.nombre(nombre);
        }
        refrescarSiCambio(uuid, datos);
        return datos;
    }

    /**
     * Si el almacen soporta versiones (MySQL) y otro servidor guardo datos mas
     * nuevos, se recargan aqui. Asi dos servidores no se pisan al reclamar el
     * mismo jugador. Se comprueba como mucho cada 2,5 s y solo si no hay
     * cambios sin guardar.
     */
    private void refrescarSiCambio(UUID uuid, DatosJugador datos) {
        if (almacen == null || !almacen.soportaVersiones() || datos.sucio()) {
            return;
        }
        long ahora = System.currentTimeMillis();
        Long ultima = comprobado.get(uuid);
        if (ultima != null && (ahora - ultima.longValue()) < COMPROBAR_CADA_MS) {
            return;
        }
        comprobado.put(uuid, Long.valueOf(ahora));
        long remota = almacen.version(uuid);
        Long conocida = versiones.get(uuid);
        if (remota > (conocida == null ? 0L : conocida.longValue())) {
            try {
                DatosJugador frescos = almacen.cargar(uuid, datos.nombre());
                cache.put(uuid, frescos);
                versiones.put(uuid, Long.valueOf(remota));
            } catch (ErrorAlmacen error) {
                plugin.getLogger().warning("Could not refresh the data of " + uuid + ": " + error.getMessage());
            }
        }
    }

    /** Recuerda la version con la que se cargo a un jugador. */
    private void recordarVersion(UUID uuid) {
        if (almacen != null && almacen.soportaVersiones()) {
            versiones.put(uuid, Long.valueOf(almacen.version(uuid)));
            comprobado.put(uuid, Long.valueOf(System.currentTimeMillis()));
        }
    }

    /** Datos de un jugador por nombre (solo si ya jugo en el servidor). */
    public DatosJugador obtenerPorNombre(String nombre) {
        org.bukkit.OfflinePlayer jugador = plugin.getServer().getOfflinePlayer(nombre);
        if (jugador == null) {
            return null;
        }
        return obtener(jugador.getUniqueId(), nombre);
    }

    /** Jugadores que tienen datos guardados. */
    public Set<UUID> conocidos() {
        try {
            return almacen.conocidos();
        } catch (ErrorAlmacen error) {
            plugin.getLogger().warning("Could not read the player list: " + error.getMessage());
            return new LinkedHashSet<UUID>();
        }
    }

    public int enMemoria() {
        return cache.size();
    }

    // ------------------------------------------------------------------
    // Escritura
    // ------------------------------------------------------------------

    /** Encola el guardado de un jugador si tiene cambios. */
    public void guardar(DatosJugador datos) {
        if (datos == null || !datos.sucio() || almacen == null) {
            return;
        }
        String texto;
        try {
            texto = me.jp.jprewards.storage.DatosCodec.aTexto(datos);
        } catch (Throwable error) {
            plugin.getLogger().warning("Could not serialize the data of " + datos.uuid() + ": " + error.getMessage());
            return;
        }
        datos.marcarLimpio();
        synchronized (cola) {
            cola.put(datos.uuid(), new String[]{datos.nombre(), texto});
        }
        if (plugin.ajustes().guardarAlReclamar()) {
            programarVolcado();
        }
    }

    /** Fuerza el guardado inmediato (sin cola) de un jugador. */
    public void guardarAhora(DatosJugador datos) {
        if (datos == null || almacen == null) {
            return;
        }
        try {
            almacen.guardar(datos);
            datos.marcarLimpio();
        } catch (ErrorAlmacen error) {
            plugin.getLogger().warning("Could not save the data of " + datos.uuid() + ": " + error.getMessage());
        }
    }

    /** Guarda y quita de memoria (al salir el jugador). */
    public void descargar(UUID uuid) {
        DatosJugador datos = cache.remove(uuid);
        if (datos == null) {
            return;
        }
        guardarAhora(datos);
        synchronized (cola) {
            cola.remove(uuid);
        }
    }

    /** Escribe en el almacen todo lo que haya en la cola. */
    public void volcar() {
        if (almacen == null) {
            return;
        }
        if (!vaciando.compareAndSet(false, true)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Map<UUID, String[]> lote = new LinkedHashMap<UUID, String[]>();
                        synchronized (cola) {
                            if (cola.isEmpty()) {
                                break;
                            }
                            lote.putAll(cola);
                            cola.clear();
                        }
                        for (Map.Entry<UUID, String[]> entrada : lote.entrySet()) {
                            try {
                                almacen.guardarTexto(entrada.getKey(), entrada.getValue()[0], entrada.getValue()[1]);
                            } catch (ErrorAlmacen error) {
                                plugin.getLogger().warning("Could not save a " + entrada.getKey() + ": "
                                        + error.getMessage());
                            }
                        }
                    }
                } finally {
                    vaciando.set(false);
                    synchronized (cola) {
                        if (!cola.isEmpty()) {
                            programarVolcado();
                        }
                    }
                }
            }
        });
    }

    private void programarVolcado() {
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                volcar();
            }
        }, 1L);
    }

    /** Guarda TODO de forma sincrona: se usa al apagar el servidor. */
    public void guardarTodo() {
        if (almacen == null) {
            return;
        }
        List<DatosJugador> sucios = new ArrayList<DatosJugador>();
        Collection<DatosJugador> todos = new ArrayList<DatosJugador>(cache.values());
        for (DatosJugador datos : todos) {
            if (datos.sucio()) {
                sucios.add(datos);
            }
        }
        try {
            almacen.guardarVarios(sucios);
            for (DatosJugador datos : sucios) {
                datos.marcarLimpio();
            }
        } catch (ErrorAlmacen error) {
            plugin.getLogger().severe("Could not save the data on shutdown: " + error.getMessage());
        }
        // Lo que quedara en la cola (de jugadores ya descargados) se escribe tambien.
        Map<UUID, String[]> lote = new LinkedHashMap<UUID, String[]>();
        synchronized (cola) {
            lote.putAll(cola);
            cola.clear();
        }
        for (Map.Entry<UUID, String[]> entrada : lote.entrySet()) {
            try {
                almacen.guardarTexto(entrada.getKey(), entrada.getValue()[0], entrada.getValue()[1]);
            } catch (ErrorAlmacen error) {
                plugin.getLogger().warning("Could not save a " + entrada.getKey() + ": " + error.getMessage());
            }
        }
    }

    /** Cierra el almacen (al desactivar el plugin). */
    public void cerrar() {
        cancelarAutoguardado();
        guardarTodo();
        if (almacen != null) {
            almacen.cerrar();
        }
    }

    /** Cambia el almacen en caliente (lo usa /rewards migrate al terminar). */
    public void recargarAlmacen() {
        guardarTodo();
        if (almacen != null) {
            almacen.cerrar();
        }
        cache.clear();
        synchronized (cola) {
            cola.clear();
        }
        iniciar();
    }

    // ------------------------------------------------------------------
    // Info
    // ------------------------------------------------------------------

    public TipoAlmacen tipoReal() {
        return tipoReal;
    }

    public String describir() {
        return almacen == null ? "-" : almacen.describir();
    }

    public Almacen almacen() {
        return almacen;
    }
}
