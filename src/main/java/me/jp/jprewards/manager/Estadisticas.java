package me.jp.jprewards.manager;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.DatosJugador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Ranking de jugadores (/rewards top).
 *
 * La lista se calcula en segundo plano leyendo los datos guardados, asi que
 * no bloquea el servidor ni depende de que los jugadores esten conectados.
 * El resultado se guarda en cache unos minutos para no releer todo en cada
 * consulta.
 */
public final class Estadisticas {

    /** Un puesto del ranking. */
    public static final class Entrada {

        private final UUID uuid;
        private final String nombre;
        private final int valor;

        Entrada(UUID uuid, String nombre, int valor) {
            this.uuid = uuid;
            this.nombre = nombre;
            this.valor = valor;
        }

        public UUID uuid() {
            return uuid;
        }

        public String nombre() {
            return nombre == null || nombre.isEmpty() ? uuid.toString().substring(0, 8) : nombre;
        }

        public int valor() {
            return valor;
        }
    }

    /** Tipos de ranking disponibles (nombres que escribe el jugador). */
    public static final String[] TIPOS = {"streak", "total", "weeks", "milestones", "bonuses"};

    /** Aviso de resultado (se ejecuta en el hilo principal). */
    public interface Aviso {
        void listo(List<Entrada> entradas);
    }

    private final JPRewards plugin;
    private volatile List<Entrada> cache = new ArrayList<Entrada>();
    private volatile String tipoCache = "";
    private volatile long momentoCache;
    private volatile boolean calculando;

    public Estadisticas(JPRewards plugin) {
        this.plugin = plugin;
    }

    /** true si el texto es un tipo de ranking valido. */
    public static boolean tipoValido(String tipo) {
        for (String candidato : TIPOS) {
            if (candidato.equalsIgnoreCase(tipo)) {
                return true;
            }
        }
        return false;
    }

    /** Indice de un tipo (para los mensajes de ayuda). */
    public static String tipos() {
        return String.join(", ", TIPOS);
    }

    /** Ranking en cache (puede estar vacio si aun no se calculo). */
    public List<Entrada> cache() {
        return new ArrayList<Entrada>(cache);
    }

    /**
     * Calcula el ranking. Si ya hay uno reciente del mismo tipo se devuelve al
     * instante; si no, se calcula en segundo plano y se avisa al terminar.
     */
    public void calcular(final String tipo, final int cantidad, final Aviso aviso) {
        final String limpio = tipo.toLowerCase(java.util.Locale.ROOT);
        long ahora = System.currentTimeMillis();
        if (limpio.equals(tipoCache) && !cache.isEmpty() && (ahora - momentoCache) < 60000L) {
            aviso.listo(cache());
            return;
        }
        if (calculando) {
            plugin.getLogger().info("A leaderboard is already being calculated; waiting for it to finish.");
            return;
        }
        calculando = true;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                final List<Entrada> calculado = new ArrayList<Entrada>();
                try {
                    for (UUID uuid : plugin.datos().conocidos()) {
                        try {
                            // Se lee directo del almacen: es un hilo aparte y no
                            // toca la cache del hilo principal.
                            DatosJugador datos = plugin.datos().almacen().cargar(uuid, null);
                            calculado.add(new Entrada(uuid, datos.nombre(), valor(datos, limpio)));
                        } catch (Throwable error) {
                            plugin.getLogger().warning("Could not read the data of " + uuid + " para el ranking: "
                                    + error.getMessage());
                        }
                    }
                } catch (Throwable error) {
                    plugin.getLogger().warning("Could not calculate el ranking: " + error.getMessage());
                }
                Collections.sort(calculado, new Comparator<Entrada>() {
                    @Override
                    public int compare(Entrada uno, Entrada otro) {
                        return Integer.compare(otro.valor(), uno.valor());
                    }
                });
                final List<Entrada> recortado = new ArrayList<Entrada>();
                for (int i = 0; i < calculado.size() && i < cantidad; i++) {
                    if (calculado.get(i).valor() > 0) {
                        recortado.add(calculado.get(i));
                    }
                }
                cache = recortado;
                tipoCache = limpio;
                momentoCache = System.currentTimeMillis();
                calculando = false;
                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        aviso.listo(recortado);
                    }
                });
            }
        });
    }

    private int valor(DatosJugador datos, String tipo) {
        if ("total".equals(tipo)) {
            return datos.total();
        }
        if ("weeks".equals(tipo)) {
            return datos.totalSemanal();
        }
        if ("milestones".equals(tipo)) {
            return datos.hitosHistoricos();
        }
        if ("bonuses".equals(tipo)) {
            return datos.bonusReclamados().size();
        }
        return datos.mejorRacha();
    }

    /** Limpia la cache (al recargar o migrar datos). */
    public void limpiar() {
        cache = new ArrayList<Entrada>();
        tipoCache = "";
        momentoCache = 0L;
    }
}
