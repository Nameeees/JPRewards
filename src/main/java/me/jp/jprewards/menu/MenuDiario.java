package me.jp.jprewards.menu;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.config.Mensajes;
import me.jp.jprewards.model.DatosJugador;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Menu del ciclo diario: los N dias del ciclo como cabezas, centrados y con
 * paginacion automatica si no caben.
 *
 * Estados de cada dia: 'hoy' (se puede reclamar), 'reclamado' y 'bloqueado'.
 */
public final class MenuDiario extends Menu {

    public MenuDiario(JPRewards plugin, Player jugador, int pagina) {
        super(plugin, jugador, "menu-diario", pagina);
    }

    @Override
    protected int paginas() {
        int capacidad = Math.max(1, capacidadPagina());
        return Math.max(1, (int) Math.ceil((double) plugin.recompensas().diasDelCiclo() / capacidad));
    }

    @Override
    protected void construir() {
        DatosJugador datos = plugin.recompensas().datos(jugador);
        Map<String, String> ph = plugin.menuManager().placeholders(jugador, datos);

        int total = plugin.recompensas().diasDelCiclo();
        int capacidad = Math.max(1, capacidadPagina());
        int desde = (pagina - 1) * capacidad;
        int hasta = Math.min(total, desde + capacidad);

        boolean puede = plugin.recompensas().puedeReclamarDiaria(datos);
        int reclamadosEnCiclo = puede ? (datos.total() % total) : (((datos.total() - 1) % total) + 1);

        List<Integer> slots = slotsAutomaticos(Math.max(1, hasta - desde));
        for (int indice = desde; indice < hasta; indice++) {
            int dia = indice + 1;
            String estado;
            if (puede && dia == reclamadosEnCiclo + 1) {
                estado = "hoy";
            } else if (dia <= reclamadosEnCiclo) {
                estado = "reclamado";
            } else {
                estado = "bloqueado";
            }
            int slot = indice - desde < slots.size() ? slots.get(indice - desde).intValue() : -1;
            if (slot < 0) {
                continue;
            }
            pintarRecompensa("dia", slot, plugin.recompensas().recompensaDelDia(dia), estado, ph,
                    Mensajes.ph("dia", String.valueOf(dia), "dias", String.valueOf(total)),
                    "nada");
        }

        marco();
        paginacion();
        navegacion();
    }
}
