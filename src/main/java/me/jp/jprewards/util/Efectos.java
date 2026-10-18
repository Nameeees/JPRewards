package me.jp.jprewards.util;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.EfectosPremio;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Efectos al reclamar una recompensa: sonido, particulas, fuegos
 * artificiales, titulo de pantalla, action bar, mensajes y anuncio global.
 *
 * Cada recompensa puede definir los suyos en recompensas/*.yml (seccion
 * 'efectos'); lo que no defina usa lo de config.yml.
 */
public final class Efectos {

    private final JPRewards plugin;

    public Efectos(JPRewards plugin) {
        this.plugin = plugin;
    }

    /**
     * Aplica los efectos de un reclamo.
     *
     * @param jugador        quien reclama
     * @param efectos        efectos del premio (nunca null)
     * @param nombrePremio   nombre visible de la recompensa
     * @param placeholders   placeholders extra ({recompensa}, {premio}, ...)
     */
    public void celebrar(Player jugador, EfectosPremio efectos, String nombrePremio, Map<String, String> placeholders) {
        if (jugador == null || efectos == null) {
            return;
        }
        Map<String, String> ph = placeholders == null
                ? new java.util.LinkedHashMap<String, String>()
                : new java.util.LinkedHashMap<String, String>(placeholders);
        ph.put("premio", nombrePremio);
        ph.put("recompensa", nombrePremio);

        // 1) Sonido
        plugin.sonidos().reproducir(jugador, efectos.sonido() == null ? plugin.ajustes().sonidoExito() : efectos.sonido(),
                efectos.volumen(), efectos.tono());

        // 2) Particulas
        if (plugin.ajustes().particulasActivas() && efectos.particula() != null) {
            Location ubicacion = jugador.getLocation().add(0.0D, 1.0D, 0.0D);
            Compat.particula(ubicacion, efectos.particula(), Math.max(1, efectos.particulas()), 0.6D);
        }

        // 3) Fuegos artificiales
        if (efectos.fuegos() || plugin.ajustes().fuegosActivos()) {
            List<String> colores = efectos.fuegosColores().isEmpty() ? plugin.ajustes().fuegosColores()
                    : efectos.fuegosColores();
            int potencia = efectos.fuegos() ? efectos.fuegosPotencia() : plugin.ajustes().fuegosPotencia();
            Compat.fuegoArtificial(jugador.getLocation(), colores, potencia);
        }

        // 4) Titulo de pantalla
        boolean titulo = efectos.titulo() == null ? plugin.ajustes().titulosActivos() : efectos.titulo().booleanValue();
        if (titulo && plugin.ajustes().titulosActivos()) {
            Compat.titulo(jugador, plugin.mensajes().texto("titulos.reclamar-arriba", ph, jugador),
                    plugin.mensajes().texto("titulos.reclamar-abajo", ph, jugador));
        }

        // 5) Action bar del premio
        if (efectos.accionBar() != null) {
            String texto = Texto.color(Texto.reemplazar(efectos.accionBar(), ph));
            Compat.actionBar(jugador, texto);
        }

        // 6) Mensajes extra al jugador
        for (String linea : efectos.mensajes()) {
            if (Texto.vacioVisible(linea)) {
                continue;
            }
            jugador.sendMessage(plugin.mensajes().prefijo() + Texto.color(Texto.reemplazar(linea, ph)));
        }

        // 7) Anuncio a todo el servidor
        if (efectos.broadcast() != null && !efectos.broadcast().trim().isEmpty()) {
            Map<String, String> phBroadcast = new java.util.LinkedHashMap<String, String>(ph);
            phBroadcast.put("jugador", jugador.getName());
            Bukkit.broadcastMessage(Texto.color(Texto.reemplazar(efectos.broadcast(), phBroadcast)));
        }
    }
}
