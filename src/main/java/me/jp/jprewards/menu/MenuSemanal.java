package me.jp.jprewards.menu;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.config.Mensajes;
import me.jp.jprewards.model.DatosJugador;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Menu del ciclo semanal: los bloques del ciclo como cabezas, con estados
 * 'disponible', 'reclamado' y 'bloqueado', mas una tarjeta de racha semanal.
 */
public final class MenuSemanal extends Menu {

    public MenuSemanal(JPRewards plugin, Player jugador, int pagina) {
        super(plugin, jugador, "menu-semanal", pagina);
    }

    @Override
    protected int paginas() {
        int capacidad = Math.max(1, capacidadPagina());
        return Math.max(1, (int) Math.ceil((double) plugin.recompensas().semanasDelCiclo() / capacidad));
    }

    @Override
    protected void construir() {
        DatosJugador datos = plugin.recompensas().datos(jugador);
        Map<String, String> ph = plugin.menuManager().placeholders(jugador, datos);

        int total = plugin.recompensas().semanasDelCiclo();
        int capacidad = Math.max(1, capacidadPagina());
        int desde = (pagina - 1) * capacidad;
        int hasta = Math.min(total, desde + capacidad);

        boolean puede = plugin.recompensas().puedeReclamarSemanal(datos);
        int reclamadasEnCiclo = puede ? (datos.totalSemanal() % total) : (((datos.totalSemanal() - 1) % total) + 1);

        List<Integer> slots = slotsAutomaticos(Math.max(1, hasta - desde));
        for (int indice = desde; indice < hasta; indice++) {
            int semana = indice + 1;
            String estado;
            if (puede && semana == reclamadasEnCiclo + 1) {
                estado = "disponible";
            } else if (semana <= reclamadasEnCiclo) {
                estado = "reclamado";
            } else {
                estado = "bloqueado";
            }
            int slot = indice - desde < slots.size() ? slots.get(indice - desde).intValue() : -1;
            if (slot < 0) {
                continue;
            }
            pintarRecompensa("semana", slot, plugin.recompensas().recompensaDeSemana(semana), estado, ph,
                    Mensajes.ph("semana", String.valueOf(semana), "semanas", String.valueOf(total)),
                    "nada");
        }

        marco();
        pintarTarjeta("racha", ph);
        paginacion();
        navegacion();
    }

    /** Tarjeta informativa que solo se coloca si su slot sigue libre. */
    private void pintarTarjeta(String clave, Map<String, String> ph) {
        me.jp.jprewards.config.ItemDef def = plugin.menus().boton("menu-semanal", clave);
        if (def == null || def.slot() < 0) {
            return;
        }
        if (getInventory().getItem(def.slot()) != null) {
            return;
        }
        pintarBoton(clave, ph);
    }
}
