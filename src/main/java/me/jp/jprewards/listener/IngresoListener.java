package me.jp.jprewards.listener;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.config.Mensajes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Entrada y salida de jugadores:
 *
 *   - al entrar: entrega los premios que no cabian en el inventario, avisa de
 *     lo que tiene disponible (con un pequeno retraso para que el jugador ya
 *     este en el mundo) y, si esta activado, reclama la diaria solo;
 *   - al salir: guarda y descarga sus datos.
 */
public final class IngresoListener implements Listener {

    private final JPRewards plugin;

    public IngresoListener(JPRewards plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void alEntrar(PlayerJoinEvent evento) {
        final Player jugador = evento.getPlayer();
        // Se cargan sus datos ya: crea el archivo si es la primera vez.
        plugin.recompensas().datos(jugador);

        long retraso = Math.max(0, plugin.ajustes().avisoRetraso()) * 20L;
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (!jugador.isOnline()) {
                    return;
                }
                plugin.recompensas().entregarPendientes(jugador);
                plugin.recompensas().avisarDisponibles(jugador);
                avisarActualizacion(jugador);
            }
        }, retraso);

        if (plugin.ajustes().autoReclamar()) {
            boolean permitido = !plugin.ajustes().autoReclamarConPermiso()
                    || jugador.hasPermission("jprewards.autoclaim");
            if (permitido) {
                long espera = retraso + Math.max(0, plugin.ajustes().autoReclamarRetraso()) * 20L;
                plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (!jugador.isOnline()) {
                            return;
                        }
                        if (plugin.recompensas().reclamarDiaria(jugador)) {
                            plugin.mensajes().enviar(jugador, "general.autoreclamado");
                        }
                    }
                }, espera);
            }
        }
    }

    private void avisarActualizacion(Player jugador) {
        if (!jugador.hasPermission("jprewards.update") || !plugin.ajustes().avisarAdminsActualizacion()) {
            return;
        }
        if (plugin.hookActualizaciones() == null || !plugin.hookActualizaciones().hayNueva()) {
            return;
        }
        plugin.mensajes().enviar(jugador, "general.actualizacion",
                Mensajes.ph("version", plugin.hookActualizaciones().ultimaVersion(),
                        "actual", plugin.getDescription().getVersion()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void alSalir(PlayerQuitEvent evento) {
        plugin.datos().descargar(evento.getPlayer().getUniqueId());
        plugin.recompensas().limpiarAvisos(evento.getPlayer().getUniqueId());
    }
}
