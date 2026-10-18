package me.jp.jprewards.hook;

import me.jp.jprewards.JPRewards;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Puente con PlaceholderAPI.
 *
 * PlaceholderAPI es OPCIONAL: si no esta instalado el plugin funciona igual,
 * solo deja de resolver los %placeholders% que escribas en sus textos. Esta
 * clase existe para que ninguna otra tenga que saber si PAPI esta o no:
 * comprueba su presencia y, solo entonces, carga la clase que usa su API.
 */
public final class PuentePlaceholders {

    private static Boolean disponible;
    private static java.lang.reflect.Method metodoAplicar;

    private PuentePlaceholders() {
    }

    /** true si PlaceholderAPI esta instalado y activo. */
    public static boolean disponible() {
        if (disponible == null) {
            disponible = Boolean.valueOf(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null);
        }
        return disponible.booleanValue();
    }

    /** Registra la expansion %jprewards_...% (si PAPI esta instalado). */
    public static void registrar(JPRewards plugin) {
        disponible = null;
        if (!disponible()) {
            plugin.getLogger().info("PlaceholderAPI is not installed: the %jprewards_...% placeholders will not be"
                    + " available (everything else works the same).");
            return;
        }
        try {
            Class<?> tipo = Class.forName("me.jp.jprewards.hook.Placeholders");
            Object instancia = tipo.getConstructor(JPRewards.class).newInstance(plugin);
            tipo.getMethod("registrar").invoke(instancia);
        } catch (Throwable error) {
            plugin.getLogger().warning("Could not register la expansion de PlaceholderAPI: " + error.getMessage());
        }
    }

    /** Aplica los %placeholders% de otros plugins a un texto (si PAPI esta instalado). */
    public static String aplicar(Player jugador, String texto) {
        if (jugador == null || texto == null || texto.indexOf('%') < 0 || !disponible()) {
            return texto;
        }
        try {
            if (metodoAplicar == null) {
                // Se busca una sola vez: este metodo se llama en cada mensaje.
                metodoAplicar = Class.forName("me.jp.jprewards.hook.Placeholders")
                        .getMethod("aplicar", Player.class, String.class);
            }
            return (String) metodoAplicar.invoke(null, jugador, texto);
        } catch (Throwable error) {
            return texto;
        }
    }
}
