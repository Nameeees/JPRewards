package me.jp.jprewards.manager;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.config.Mensajes;
import me.jp.jprewards.model.DatosJugador;
import me.jp.jprewards.storage.Almacen;
import me.jp.jprewards.storage.AlmacenSql;
import me.jp.jprewards.storage.AlmacenYaml;
import me.jp.jprewards.storage.TipoAlmacen;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Migracion de datos entre YAML y la base de datos (/rewards migrate).
 *
 * Como se usa (igual que en JPPlayerVaults):
 *   1. Cambia almacenamiento.tipo en config.yml al destino y reinicia.
 *      El plugin empieza a usar el almacen nuevo (todavia vacio).
 *   2. Ejecuta /rewards migrate confirm: copia los datos del almacen
 *      ANTERIOR al que estas usando ahora.
 *
 * El origen es siempre "el otro" almacen, asi que funciona en los dos
 * sentidos sin tener que cambiar nada mas. Si un jugador ya existe en el
 * destino se sobrescribe (por eso hay que escribir 'confirmar').
 */
public final class Migrador {

    private final JPRewards plugin;

    public Migrador(JPRewards plugin) {
        this.plugin = plugin;
    }

    /** Almacen del tipo indicado, con la configuracion actual. */
    private Almacen crear(TipoAlmacen tipo) {
        if (tipo == TipoAlmacen.MYSQL) {
            return new AlmacenSql(plugin, plugin.ajustes().sql());
        }
        return new AlmacenYaml(plugin, plugin.ajustes().almacenCarpeta(),
                "unico".equals(plugin.ajustes().almacenModo()));
    }

    /**
     * Copia todos los datos del almacen anterior al que se esta usando ahora.
     *
     * @param sobrescribir true = pisa los jugadores que ya existan en el destino
     */
    public void migrar(final CommandSender emisor, final boolean sobrescribir) {
        final TipoAlmacen destino = plugin.datos().tipoReal();
        final TipoAlmacen origen = destino == TipoAlmacen.MYSQL ? TipoAlmacen.YAML : TipoAlmacen.MYSQL;

        if (plugin.ajustes().almacenTipo() != destino) {
            // La base de datos no respondio al arrancar y se sigue con YAML.
            plugin.mensajes().enviar(emisor, "comando.migrar-error",
                    Mensajes.ph("error", "el almacenamiento configurado (" + plugin.ajustes().almacenTipo().clave()
                            + ") no esta activo; check la consola del arranque"));
            return;
        }

        plugin.mensajes().enviar(emisor, "comando.migrar-inicio",
                Mensajes.ph("origen", origen.clave(), "destino", destino.clave()));
        plugin.datos().guardarTodo();

        final Almacen meta = plugin.datos().almacen();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                final int[] contadores = new int[3]; // copiados, omitidos, errores
                List<UUID> jugadores = new ArrayList<UUID>();
                Almacen fuente = null;
                try {
                    fuente = crear(origen);
                    fuente.iniciar();
                    jugadores.addAll(fuente.conocidos());
                } catch (Throwable error) {
                    plugin.getLogger().warning("Could not open el almacen de origen (" + origen.clave() + "): "
                            + error.getMessage());
                    avisar(emisor, "comando.migrar-error", Mensajes.ph("error", String.valueOf(error.getMessage())));
                    return;
                }
                for (UUID uuid : jugadores) {
                    try {
                        DatosJugador datos = fuente.cargar(uuid, null);
                        boolean vacio = datos.total() == 0 && datos.racha() == 0 && datos.totalSemanal() == 0
                                && datos.diasMes() == 0 && datos.hitosReclamados().isEmpty()
                                && datos.bonusReclamados().isEmpty();
                        if (vacio) {
                            contadores[1]++;
                            continue;
                        }
                        if (!sobrescribir) {
                            DatosJugador existente = meta.cargar(uuid, null);
                            if (existente != null && existente.total() > 0) {
                                contadores[1]++;
                                continue;
                            }
                        }
                        meta.guardar(datos);
                        contadores[0]++;
                    } catch (Throwable error) {
                        contadores[2]++;
                        plugin.getLogger().warning("Could not migrate a " + uuid + ": " + error.getMessage());
                    }
                }
                try {
                    fuente.cerrar();
                } catch (Throwable ignorado) {
                    // nada que hacer
                }
                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Map<String, String> ph = Mensajes.ph("copiados", String.valueOf(contadores[0]),
                                "omitidos", String.valueOf(contadores[1]), "errores", String.valueOf(contadores[2]));
                        ph.put("origen", origen.clave());
                        ph.put("destino", destino.clave());
                        plugin.mensajes().enviar(emisor, "comando.migrar-fin", ph);
                        plugin.estadisticas().limpiar();
                        // Los jugadores conectados seguiran con los datos que
                        // ya tenian en memoria: se avisa para que reconecten.
                        if (contadores[0] > 0) {
                            plugin.mensajes().enviar(emisor, "comando.migrar-recordatorio",
                                    Mensajes.ph("destino", destino.clave()));
                        }
                    }
                });
            }
        });
    }

    private void avisar(final CommandSender emisor, final String clave, final Map<String, String> ph) {
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.mensajes().enviar(emisor, clave, ph);
            }
        });
    }
}
