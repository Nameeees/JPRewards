package me.jp.jprewards.menu;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.DatosJugador;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Menu generico: dibuja los botones que definas en menus.yml sin contenido
 * automatico. Sirve para crear menus propios (por ejemplo un menu de enlaces)
 * y abrirlos con 'abrir:<id>' desde cualquier otro boton o comando.
 */
public final class MenuGenerico extends Menu {

    public MenuGenerico(JPRewards plugin, Player jugador, String menuId) {
        super(plugin, jugador, menuId);
    }

    @Override
    protected void construir() {
        DatosJugador datos = plugin.recompensas().datos(jugador);
        Map<String, String> ph = plugin.menuManager().placeholders(jugador, datos);

        for (String clave : plugin.menus().botones(menuId())) {
            pintarBoton(clave, ph);
        }

        marco();
        navegacion();
    }
}
