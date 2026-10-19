package me.jp.jprewards.api;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.BonusRacha;
import me.jp.jprewards.model.DatosJugador;
import me.jp.jprewards.model.Hito;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Punto de entrada para otros plugins.
 *
 * Todo lo que hace el plugin por comando o por menu se puede hacer desde aqui,
 * y cada entrega dispara eventos (ver {@code me.jp.jprewards.api.event}):
 *
 * <pre>
 * JPRewardsAPI api = JPRewardsAPI.get();
 * if (api != null &amp;&amp; api.hasPending(player)) { ... }
 * api.claim(player, "daily");              // como /rewards claim daily
 * api.give(player, "daily", "3");          // como /rewards give
 * api.giveExtra(player, "boss-reward");    // premio de recompensas/extra.yml
 * </pre>
 *
 * Para dar una recompensa desde un evento de OTRO plugin (una muerte, un voto,
 * una mision) basta con definir el premio en {@code recompensas/extra.yml} y
 * llamar a {@link #giveExtra(Player, String)}.
 */
public final class JPRewardsAPI {

    private JPRewardsAPI() {
    }

    /** Devuelve la API, o null si el plugin no esta activo. */
    public static JPRewardsAPI get() {
        JPRewards plugin = JPRewards.getInstancia();
        return plugin == null || !plugin.isEnabled() ? null : new JPRewardsAPI();
    }

    private static JPRewards plugin() {
        return JPRewards.getInstancia();
    }

    /** true si el plugin esta cargado y funcionando. */
    public boolean enabled() {
        JPRewards plugin = plugin();
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Reclama como si el jugador usara el comando.
     *
     * @param tipo daily, weekly, monthly, streaks o all
     * @return true si se reclamo algo (aunque el premio saliera vacio por una condicion)
     */
    public boolean claim(Player jugador, String tipo) {
        if (!enabled() || jugador == null || tipo == null) {
            return false;
        }
        String limpio = tipo.toLowerCase(Locale.ROOT);
        if ("all".equals(limpio)) {
            return plugin().recompensas().reclamarTodo(jugador) > 0;
        }
        if ("daily".equals(limpio)) {
            return plugin().recompensas().reclamarDiaria(jugador);
        }
        if ("weekly".equals(limpio)) {
            return plugin().recompensas().reclamarSemanal(jugador);
        }
        if ("monthly".equals(limpio)) {
            boolean alguna = false;
            DatosJugador datos = plugin().recompensas().datos(jugador);
            for (Hito hito : plugin().recompensas().hitosDisponibles(datos)) {
                alguna = plugin().recompensas().reclamarHito(jugador, hito) || alguna;
            }
            return alguna;
        }
        if ("streaks".equals(limpio)) {
            boolean alguna = false;
            DatosJugador datos = plugin().recompensas().datos(jugador);
            for (BonusRacha bono : plugin().recompensas().bonusDisponibles(datos)) {
                alguna = plugin().recompensas().reclamarBonus(jugador, bono) || alguna;
            }
            return alguna;
        }
        return false;
    }

    /** Entrega una recompensa concreta de un ciclo (como /rewards give). */
    public boolean give(Player jugador, String tipo, String clave) {
        return enabled() && jugador != null && plugin().recompensas().regalar(jugador, tipo, clave);
    }

    /** true si hoy ya reclamo la recompensa diaria. */
    public boolean hasClaimedToday(Player jugador) {
        if (!enabled() || jugador == null) {
            return false;
        }
        return !plugin().recompensas().puedeReclamarDiaria(plugin().recompensas().datos(jugador));
    }

    /** Cuantas recompensas puede reclamar ahora mismo. */
    public int pending(Player jugador) {
        if (!enabled() || jugador == null) {
            return 0;
        }
        DatosJugador datos = plugin().recompensas().datos(jugador);
        int total = 0;
        if (plugin().recompensas().puedeReclamarDiaria(datos)) {
            total++;
        }
        if (plugin().recompensas().puedeReclamarSemanal(datos)) {
            total++;
        }
        total += plugin().recompensas().hitosDisponibles(datos).size();
        total += plugin().recompensas().bonusDisponibles(datos).size();
        return total;
    }

    /** true si tiene algo que reclamar. */
    public boolean hasPending(Player jugador) {
        return pending(jugador) > 0;
    }

    /** Datos persistentes de un jugador. */
    public DatosJugador data(UUID uuid) {
        return enabled() && uuid != null ? plugin().datos().obtener(uuid, "") : null;
    }

    /** Claves definidas en un ciclo: dias 1..N, semanas, ids de hitos o de bonus. */
    public List<String> rewardKeys(String tipo) {
        List<String> claves = new ArrayList<String>();
        if (!enabled() || tipo == null) {
            return claves;
        }
        String limpio = tipo.toLowerCase(Locale.ROOT);
        if ("daily".equals(limpio)) {
            for (Integer dia : plugin().recompensas().diasDelCicloLista()) {
                claves.add(String.valueOf(dia));
            }
        } else if ("weekly".equals(limpio)) {
            for (Integer semana : plugin().recompensas().semanasDelCicloLista()) {
                claves.add(String.valueOf(semana));
            }
        } else if ("monthly".equals(limpio)) {
            for (Hito hito : plugin().recompensas().hitos()) {
                claves.add(hito.id());
            }
        } else if ("streaks".equals(limpio)) {
            for (BonusRacha bono : plugin().recompensas().bonus()) {
                claves.add(bono.id());
            }
        }
        return claves;
    }

    /** Ids de los premios sueltos de recompensas/extra.yml. */
    public List<String> extraKeys() {
        return enabled() ? plugin().recompensas().extrasDisponibles() : new ArrayList<String>();
    }

    /**
     * Entrega un premio suelto definido en {@code recompensas/extra.yml}.
     * Es la forma de dar recompensas desde los eventos de otro plugin.
     */
    public boolean giveExtra(Player jugador, String id) {
        return enabled() && jugador != null && plugin().recompensas().regalarExtra(jugador, id);
    }

    /** Igual que {@link #giveExtra(Player, String)} pero por nombre de jugador. */
    public boolean giveExtra(String nombreJugador, String id) {
        Player jugador = Bukkit.getPlayerExact(nombreJugador == null ? "" : nombreJugador);
        return jugador != null && giveExtra(jugador, id);
    }
}
