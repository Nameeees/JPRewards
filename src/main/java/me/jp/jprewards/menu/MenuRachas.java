package me.jp.jprewards.menu;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.config.Mensajes;
import me.jp.jprewards.model.BonusRacha;
import me.jp.jprewards.model.DatosJugador;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu de bonus por racha: cada bonus se desbloquea al alcanzar N dias (o
 * semanas) seguidos y se puede reclamar una vez por racha.
 *
 * Estados: 'disponible', 'reclamado' (ya cobrado en esta racha) y
 * 'bloqueado' (todavia no llegas).
 */
public final class MenuRachas extends Menu {

    public MenuRachas(JPRewards plugin, Player jugador, int pagina) {
        super(plugin, jugador, "menu-rachas", pagina);
    }

    @Override
    protected int paginas() {
        int capacidad = Math.max(1, capacidadPagina());
        return Math.max(1, (int) Math.ceil((double) plugin.recompensas().bonus().size() / capacidad));
    }

    @Override
    protected void construir() {
        DatosJugador datos = plugin.recompensas().datos(jugador);
        Map<String, String> ph = plugin.menuManager().placeholders(jugador, datos);

        List<BonusRacha> bonus = plugin.recompensas().bonus();
        int capacidad = Math.max(1, capacidadPagina());
        int desde = (pagina - 1) * capacidad;
        int hasta = Math.min(bonus.size(), desde + capacidad);

        List<Integer> slots = slotsAutomaticos(Math.max(1, hasta - desde));
        for (int indice = desde; indice < hasta; indice++) {
            BonusRacha bono = bonus.get(indice);
            int slot = indice - desde < slots.size() ? slots.get(indice - desde).intValue() : -1;
            if (slot < 0) {
                continue;
            }
            int racha = plugin.recompensas().rachaDe(datos, bono);
            String estado;
            if (datos.bonusReclamados().contains(bono.id())) {
                estado = "reclamado";
            } else if (racha >= bono.rachaMinima()) {
                estado = "disponible";
            } else {
                estado = "bloqueado";
            }
            Map<String, String> extra = new LinkedHashMap<String, String>();
            extra.put("racha_minima", String.valueOf(bono.rachaMinima()));
            extra.put("racha_necesaria", String.valueOf(bono.rachaMinima()));
            extra.put("faltan", String.valueOf(Math.max(0, bono.rachaMinima() - racha)));
            extra.put("tipo", bono.tipo().clave());
            extra.put("bonus", bono.id());
            pintarRecompensa("bonus", slot, bono.recompensa(), estado, ph, extra, "reclamar:racha:" + bono.id());
        }

        marco();
        pintarProgreso("racha", ph);
        pintarProgreso("progreso", ph);
        paginacion();
        navegacion();
    }

    private void pintarProgreso(String clave, Map<String, String> ph) {
        me.jp.jprewards.config.ItemDef def = plugin.menus().boton("menu-rachas", clave);
        if (def == null || def.slot() < 0) {
            return;
        }
        if (getInventory().getItem(def.slot()) != null) {
            return;
        }
        pintarBoton(clave, ph);
    }
}
