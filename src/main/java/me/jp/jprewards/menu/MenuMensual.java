package me.jp.jprewards.menu;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.config.Mensajes;
import me.jp.jprewards.model.DatosJugador;
import me.jp.jprewards.model.Hito;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu de hitos mensuales: cada hito es una cabeza con estados
 * 'disponible', 'bloqueado' y 'reclamado', mas una tarjeta de progreso.
 * Los hitos se reparten solos y se paginan si son muchos.
 */
public final class MenuMensual extends Menu {

    public MenuMensual(JPRewards plugin, Player jugador, int pagina) {
        super(plugin, jugador, "menu-mensual", pagina);
    }

    @Override
    protected int paginas() {
        int capacidad = Math.max(1, capacidadPagina());
        return Math.max(1, (int) Math.ceil((double) plugin.recompensas().hitos().size() / capacidad));
    }

    @Override
    protected void construir() {
        DatosJugador datos = plugin.recompensas().datos(jugador);
        Map<String, String> ph = plugin.menuManager().placeholders(jugador, datos);

        List<Hito> hitos = plugin.recompensas().hitos();
        int capacidad = Math.max(1, capacidadPagina());
        int desde = (pagina - 1) * capacidad;
        int hasta = Math.min(hitos.size(), desde + capacidad);

        List<Integer> slots = slotsAutomaticos(Math.max(1, hasta - desde));
        for (int indice = desde; indice < hasta; indice++) {
            Hito hito = hitos.get(indice);
            int slot = indice - desde < slots.size() ? slots.get(indice - desde).intValue() : -1;
            if (slot < 0) {
                continue;
            }
            String estado;
            if (datos.hitosReclamados().contains(hito.id())) {
                estado = "reclamado";
            } else if (datos.diasMes() >= hito.diasRequeridos()) {
                estado = "disponible";
            } else {
                estado = "bloqueado";
            }
            Map<String, String> extra = new LinkedHashMap<String, String>();
            extra.put("dias", String.valueOf(hito.diasRequeridos()));
            extra.put("faltan", String.valueOf(Math.max(0, hito.diasRequeridos() - datos.diasMes())));
            extra.put("hito", hito.id());
            pintarRecompensa("hito", slot, hito.recompensa(), estado, ph, extra, "reclamar:hito:" + hito.id());
        }

        marco();
        pintarProgreso(ph);
        paginacion();
        navegacion();
    }

    private void pintarProgreso(Map<String, String> ph) {
        me.jp.jprewards.config.ItemDef def = plugin.menus().boton("menu-mensual", "progreso");
        if (def == null || def.slot() < 0) {
            return;
        }
        if (getInventory().getItem(def.slot()) != null) {
            return;
        }
        pintarBoton("progreso", ph);
    }
}
