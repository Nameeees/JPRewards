package me.jp.jprewards.menu;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.DatosJugador;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Menu de informacion: racha, totales y proximos reinicios.
 * Todas las tarjetas se definen en menus.yml (menu-info.botones), asi que se
 * pueden agregar, quitar o mover sin tocar el plugin.
 */
public final class MenuInfo extends Menu {

    public MenuInfo(JPRewards plugin, Player jugador) {
        super(plugin, jugador, "menu-info");
    }

    @Override
    protected void construir() {
        DatosJugador datos = plugin.recompensas().datos(jugador);
        Map<String, String> ph = plugin.menuManager().placeholders(jugador, datos);

        for (String clave : plugin.menus().botones("menu-info")) {
            pintarBoton(clave, ph);
        }

        marco();
        navegacion();
    }
}
