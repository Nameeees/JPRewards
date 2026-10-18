package me.jp.jprewards.util;

import me.jp.jprewards.JPRewards;
import org.bukkit.entity.Player;

/**
 * Sonidos del plugin: nombres configurables en config.yml con equivalencia
 * automatica para 1.8 (ver Compat). Si los sonidos estan desactivados en la
 * configuracion, no suena nada; si un premio define su propio sonido, manda
 * el del premio.
 */
public final class Sonidos {

    private final JPRewards plugin;

    public Sonidos(JPRewards plugin) {
        this.plugin = plugin;
    }

    public void abrir(Player jugador) {
        reproducir(jugador, plugin.ajustes().sonidoAbrir(), null, null);
    }

    public void clic(Player jugador) {
        reproducir(jugador, plugin.ajustes().sonidoClic(), null, null);
    }

    public void exito(Player jugador) {
        reproducir(jugador, plugin.ajustes().sonidoExito(), null, null);
    }

    public void error(Player jugador) {
        reproducir(jugador, plugin.ajustes().sonidoError(), null, null);
    }

    public void aviso(Player jugador) {
        reproducir(jugador, plugin.ajustes().sonidoAviso(), null, null);
    }

    /**
     * Reproduce un sonido respetando config.yml.
     *
     * @param nombre  sonido concreto (null = usa el sonido por defecto)
     * @param volumen volumen (null = el de config.yml)
     * @param tono    tono (null = el de config.yml)
     */
    public void reproducir(Player jugador, String nombre, Float volumen, Float tono) {
        if (jugador == null || !plugin.ajustes().sonidosActivos()) {
            return;
        }
        String elegido = nombre == null || nombre.trim().isEmpty() ? null : nombre.trim();
        if (elegido == null) {
            return;
        }
        float volumenFinal = volumen == null ? plugin.ajustes().volumenSonido() : volumen.floatValue();
        float tonoFinal = tono == null ? plugin.ajustes().tonoSonido() : tono.floatValue();
        Compat.sonido(jugador, elegido, limitar(volumenFinal, 0f, 1f), limitar(tonoFinal, 0.5f, 2f));
    }

    private static float limitar(float valor, float minimo, float maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }
}
