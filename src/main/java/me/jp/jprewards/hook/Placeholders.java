package me.jp.jprewards.hook;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.DatosJugador;
import me.jp.jprewards.util.Tiempo;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Expansion de PlaceholderAPI: %jprewards_<clave>%.
 *
 * Esta clase SOLO se carga si PlaceholderAPI esta instalado (la carga
 * {@link PuentePlaceholders} por reflexion), asi que el plugin sigue sin
 * depender de nada.
 *
 * Claves disponibles (documentadas tambien en el README):
 *   dia, racha, mejor_racha, total, disponible_diaria, tiempo_diaria,
 *   semana, semanas, racha_semanal, mejor_racha_semanal, total_semanal,
 *   disponible_semanal, tiempo_semanal, dias_mes, dias_mes_total, hitos,
 *   hitos_total, hitos_disponibles, bonus, bonus_total, bonus_disponibles,
 *   disponibles, reclamadas (si/no), moneda, premio_diaria, premio_semanal
 */
public final class Placeholders extends PlaceholderExpansion {

    private final JPRewards plugin;

    public Placeholders(JPRewards plugin) {
        this.plugin = plugin;
    }

    /** Registra la expansion en PlaceholderAPI. */
    public void registrar() {
        if (register()) {
            plugin.getLogger().info("PlaceholderAPI detected: %jprewards_racha%, %jprewards_dia%... are ready to use");
        } else {
            plugin.getLogger().warning("PlaceholderAPI did not accept the %jprewards_% registration (check its configuration).");
        }
    }

    /** Aplica los placeholders de otros plugins a un texto. */
    public static String aplicar(Player jugador, String texto) {
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(jugador, texto);
    }

    @Override
    public String getIdentifier() {
        return "jprewards";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer jugador, String parametro) {
        if (jugador == null || parametro == null) {
            return "";
        }
        String clave = parametro.trim().toLowerCase(java.util.Locale.ROOT);
        DatosJugador datos = plugin.datos().obtener(jugador.getUniqueId(), jugador.getName());
        datos.asegurarMes(Tiempo.claveMes(plugin.ajustes().horaReinicio()));
        Player enLinea = jugador.isOnline() ? jugador.getPlayer() : null;

        if ("dia".equals(clave)) {
            return String.valueOf(plugin.recompensas().proximoDia(datos));
        }
        if ("racha".equals(clave)) {
            return String.valueOf(datos.racha());
        }
        if ("mejor_racha".equals(clave)) {
            return String.valueOf(datos.mejorRacha());
        }
        if ("total".equals(clave)) {
            return String.valueOf(datos.total());
        }
        if ("disponible_diaria".equals(clave)) {
            return siNo(plugin.recompensas().puedeReclamarDiaria(datos));
        }
        if ("tiempo_diaria".equals(clave)) {
            return plugin.recompensas().tiempoRestante();
        }
        if ("semana".equals(clave)) {
            return String.valueOf(plugin.recompensas().proximaSemana(datos));
        }
        if ("semanas".equals(clave)) {
            return String.valueOf(plugin.recompensas().semanasDelCiclo());
        }
        if ("racha_semanal".equals(clave)) {
            return String.valueOf(datos.rachaSemanal());
        }
        if ("mejor_racha_semanal".equals(clave)) {
            return String.valueOf(datos.mejorRachaSemanal());
        }
        if ("total_semanal".equals(clave)) {
            return String.valueOf(datos.totalSemanal());
        }
        if ("disponible_semanal".equals(clave)) {
            return siNo(plugin.recompensas().puedeReclamarSemanal(datos));
        }
        if ("tiempo_semanal".equals(clave)) {
            return plugin.recompensas().tiempoRestanteSemanal();
        }
        if ("dias_mes".equals(clave)) {
            return String.valueOf(datos.diasMes());
        }
        if ("dias_mes_total".equals(clave)) {
            return String.valueOf(Tiempo.diasDelMesActual(plugin.ajustes().horaReinicio()));
        }
        if ("hitos".equals(clave)) {
            return String.valueOf(plugin.recompensas().hitosReclamados(datos));
        }
        if ("hitos_total".equals(clave)) {
            return String.valueOf(plugin.recompensas().hitos().size());
        }
        if ("hitos_disponibles".equals(clave)) {
            return String.valueOf(plugin.recompensas().hitosDisponibles(datos).size());
        }
        if ("bonus".equals(clave)) {
            return String.valueOf(plugin.recompensas().bonusReclamados(datos));
        }
        if ("bonus_total".equals(clave)) {
            return String.valueOf(plugin.recompensas().bonus().size());
        }
        if ("bonus_disponibles".equals(clave)) {
            return String.valueOf(plugin.recompensas().bonusDisponibles(datos).size());
        }
        if ("disponibles".equals(clave)) {
            int total = 0;
            if (plugin.recompensas().puedeReclamarDiaria(datos)) {
                total++;
            }
            if (plugin.recompensas().puedeReclamarSemanal(datos)) {
                total++;
            }
            total += plugin.recompensas().hitosDisponibles(datos).size();
            total += plugin.recompensas().bonusDisponibles(datos).size();
            return String.valueOf(total);
        }
        if ("reclamadas".equals(clave)) {
            return plugin.recompensas().puedeReclamarDiaria(datos) ? "no" : "si";
        }
        if ("moneda".equals(clave)) {
            return plugin.ajustes().moneda();
        }
        if ("premio_diaria".equals(clave)) {
            return nombre(plugin.recompensas().recompensaDelDia(plugin.recompensas().proximoDia(datos)), enLinea);
        }
        if ("premio_semanal".equals(clave)) {
            return nombre(plugin.recompensas().recompensaDeSemana(plugin.recompensas().proximaSemana(datos)), enLinea);
        }
        // Formato clave_jugador: %jprewards_racha_Notch% (solo jugadores en linea).
        int separador = clave.lastIndexOf('_');
        if (separador > 0) {
            String base = clave.substring(0, separador);
            String nombreJugador = parametro.substring(separador + 1);
            Player otro = plugin.getServer().getPlayerExact(nombreJugador);
            if (otro != null && !otro.getUniqueId().equals(jugador.getUniqueId())) {
                return onRequest(otro, base);
            }
        }
        return null;
    }

    private String nombre(me.jp.jprewards.model.Recompensa recompensa, Player jugador) {
        if (recompensa == null) {
            return "";
        }
        String nombre = jugador == null ? recompensa.nombrePorDefecto() : recompensa.nombrePara(jugador);
        return nombre == null ? "" : me.jp.jprewards.util.Texto.plano(nombre);
    }

    private static String siNo(boolean valor) {
        return valor ? "si" : "no";
    }
}
