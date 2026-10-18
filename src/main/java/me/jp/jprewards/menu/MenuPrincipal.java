package me.jp.jprewards.menu;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.BonusRacha;
import me.jp.jprewards.model.DatosJugador;
import me.jp.jprewards.model.Hito;
import me.jp.jprewards.model.Recompensa;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu principal (el hub del plugin).
 *
 * Cada boton se dibuja segun el estado del jugador y se define entero en
 * menus.yml: si agregas un boton con un nombre nuevo, se dibuja igual con su
 * texto y su accion (asi se pueden crear botones propios sin tocar el
 * plugin).
 */
public final class MenuPrincipal extends Menu {

    public MenuPrincipal(JPRewards plugin, Player jugador) {
        super(plugin, jugador, "menu-principal");
    }

    @Override
    protected void construir() {
        DatosJugador datos = plugin.recompensas().datos(jugador);
        Map<String, String> ph = plugin.menuManager().placeholders(jugador, datos);

        for (String clave : plugin.menus().botones("menu-principal")) {
            if ("diaria".equals(clave)) {
                diaria(datos, ph);
            } else if ("semanal".equals(clave)) {
                semanal(datos, ph);
            } else if ("mensual".equals(clave)) {
                mensual(datos, ph);
            } else if ("rachas".equals(clave) || "bonus".equals(clave)) {
                rachas(datos, ph);
            } else if ("reclamar-todo".equals(clave) || "reclamar_todo".equals(clave)) {
                reclamarTodo(datos, ph);
            } else {
                // Boton propio del servidor: se dibuja tal cual con su accion.
                pintarBoton(clave, ph);
            }
        }

        marco();
        navegacion();
    }

    private void diaria(DatosJugador datos, Map<String, String> ph) {
        int dia = plugin.recompensas().proximoDia(datos);
        String estado;
        if (!plugin.recompensas().hayDiaria()) {
            estado = "bloqueada";
        } else if (plugin.recompensas().puedeReclamarDiaria(datos)) {
            estado = "disponible";
        } else {
            estado = "reclamada";
        }
        pintarRecompensa("diaria", slot("diaria", 20), plugin.recompensas().recompensaDelDia(dia), estado, ph,
                me.jp.jprewards.config.Mensajes.ph("dia", String.valueOf(dia), "dias",
                        String.valueOf(plugin.recompensas().diasDelCiclo())),
                "reclamar:diaria");
    }

    private void semanal(DatosJugador datos, Map<String, String> ph) {
        int semana = plugin.recompensas().proximaSemana(datos);
        String estado;
        if (!plugin.recompensas().haySemanal()) {
            estado = "bloqueada";
        } else if (plugin.recompensas().puedeReclamarSemanal(datos)) {
            estado = "disponible";
        } else {
            estado = "reclamada";
        }
        pintarRecompensa("semanal", slot("semanal", 22), plugin.recompensas().recompensaDeSemana(semana), estado, ph,
                me.jp.jprewards.config.Mensajes.ph("semana", String.valueOf(semana)), "reclamar:semanal");
    }

    private void mensual(DatosJugador datos, Map<String, String> ph) {
        List<Hito> disponibles = plugin.recompensas().hitosDisponibles(datos);
        String estado;
        if (!plugin.recompensas().hayMensual()) {
            estado = "bloqueada";
        } else if (!disponibles.isEmpty()) {
            estado = "disponible";
        } else if (plugin.recompensas().hitosReclamados(datos) >= plugin.recompensas().hitos().size()
                && !plugin.recompensas().hitos().isEmpty()) {
            estado = "completada";
        } else {
            estado = "bloqueada";
        }
        pintarRecompensa("mensual", slot("mensual", 24), null, estado, ph, null, "abrir:menu-mensual");
    }

    private void rachas(DatosJugador datos, Map<String, String> ph) {
        List<BonusRacha> disponibles = plugin.recompensas().bonusDisponibles(datos);
        String estado;
        if (!plugin.recompensas().hayRachas()) {
            estado = "bloqueada";
        } else if (!disponibles.isEmpty()) {
            estado = "disponible";
        } else if (plugin.recompensas().bonusReclamados(datos) >= plugin.recompensas().bonus().size()
                && !plugin.recompensas().bonus().isEmpty()) {
            estado = "completada";
        } else {
            estado = "bloqueada";
        }
        Map<String, String> extra = new LinkedHashMap<String, String>();
        extra.put("bonus_disponibles", String.valueOf(disponibles.size()));
        pintarRecompensa("rachas", slot("rachas", 30), null, estado, ph, extra, "abrir:menu-rachas");
    }

    private void reclamarTodo(DatosJugador datos, Map<String, String> ph) {
        int disponibles = plugin.recompensas().disponibles(jugador);
        String estado = disponibles > 0 ? "disponible" : "nada";
        Map<String, String> extra = new LinkedHashMap<String, String>();
        extra.put("cantidad", String.valueOf(disponibles));
        extra.put("disponibles", String.valueOf(disponibles));
        pintarRecompensa("reclamar-todo", slot("reclamar-todo", 32), null, estado, ph, extra, "reclamar:todo");
    }

    /** Slot configurado de un boton (o el de reserva). */
    private int slot(String clave, int porDefecto) {
        me.jp.jprewards.config.ItemDef def = plugin.menus().boton("menu-principal", clave);
        return def == null || def.slot() < 0 ? porDefecto : def.slot();
    }
}
