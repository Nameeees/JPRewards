package me.jp.jprewards.config;

import me.jp.jprewards.JPRewards;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Lectura de menus.yml: titulos, filas, navegacion, botones (con variantes
 * por estado), paginacion y plantillas de la vista previa de premios.
 *
 * Los menus y sus botones se pueden AMPLIAR sin tocar codigo: cualquier
 * boton que definas dentro de "botones:" se dibuja con su accion
 * (abrir:menu, reclamar:..., comando:..., mensaje:...).
 */
public final class MenuArchivo {

    private final JPRewards plugin;
    private FileConfiguration archivo;

    public MenuArchivo(JPRewards plugin) {
        this.plugin = plugin;
    }

    /** Carga (o recarga) menus.yml desde la carpeta del plugin. */
    public void cargar() {
        archivo = plugin.archivos().cargar(plugin.ajustes().menusArchivo());
        if (archivo == null) {
            archivo = plugin.archivos().cargarRecurso("menus.yml");
            plugin.getLogger().warning("Could not read " + plugin.ajustes().menusArchivo()
                    + "; the menus bundled in the .jar will be used.");
        }
    }

    public FileConfiguration archivo() {
        return archivo;
    }

    public boolean existe(String menuId) {
        return archivo != null && archivo.isConfigurationSection(menuId);
    }

    public String titulo(String menuId) {
        return archivo == null ? "&dJPRewards" : archivo.getString(menuId + ".titulo", "&dJPRewards");
    }

    public int filas(String menuId) {
        int filas = archivo == null ? 5 : archivo.getInt(menuId + ".filas", 5);
        return Math.max(1, Math.min(6, filas));
    }

    /** Navegacion compartida: volver-slot / cerrar-slot (0 = oculto). */
    public int navegacion(String menuId, String clave, int porDefecto) {
        return archivo == null ? porDefecto : archivo.getInt(menuId + ".navegacion." + clave, porDefecto);
    }

    /** Boton comun a todos los menus (marco, volver, cerrar). */
    public ItemDef comun(String clave) {
        ConfigurationSection seccion = archivo == null ? null : archivo.getConfigurationSection("comun." + clave);
        return seccion == null ? null : new ItemDef(seccion, 0);
    }

    /** Nombres de los botones comunes definidos en comun:. */
    public List<String> comunes() {
        List<String> nombres = new ArrayList<String>();
        ConfigurationSection seccion = archivo == null ? null : archivo.getConfigurationSection("comun");
        if (seccion != null) {
            for (String clave : seccion.getKeys(false)) {
                if (seccion.isConfigurationSection(clave)) {
                    nombres.add(clave);
                }
            }
        }
        return nombres;
    }

    /** Boton de un menu concreto. */
    public ItemDef boton(String menuId, String clave) {
        ConfigurationSection seccion = archivo == null ? null
                : archivo.getConfigurationSection(menuId + ".botones." + clave);
        if (seccion == null) {
            return null;
        }
        return new ItemDef(seccion, seccion.getInt("slot", -1));
    }

    /** Todos los botones de un menu (permite agregar botones propios). */
    public List<String> botones(String menuId) {
        List<String> nombres = new ArrayList<String>();
        ConfigurationSection seccion = archivo == null ? null : archivo.getConfigurationSection(menuId + ".botones");
        if (seccion != null) {
            for (String clave : seccion.getKeys(false)) {
                if (seccion.isConfigurationSection(clave)) {
                    nombres.add(clave);
                }
            }
        }
        return nombres;
    }

    /** Plantilla de la vista previa de premios: comun.preview.<clave>. */
    public String preview(String clave) {
        return archivo == null ? "" : archivo.getString("comun.preview." + clave, "");
    }

    /** Slot donde empiezan los items automaticos de un menu (ciclos). */
    public int slotInicial(String menuId, int porDefecto) {
        return archivo == null ? porDefecto : archivo.getInt(menuId + ".slot-inicial", porDefecto);
    }

    /** Slots concretos para los items automaticos (si se definen). */
    public List<Integer> slotsAutomaticos(String menuId) {
        List<Integer> slots = new ArrayList<Integer>();
        if (archivo == null) {
            return slots;
        }
        if (archivo.isList(menuId + ".slots")) {
            for (Object valor : archivo.getList(menuId + ".slots")) {
                if (valor != null) {
                    try {
                        slots.add(Integer.parseInt(String.valueOf(valor).trim()));
                    } catch (NumberFormatException ignorado) {
                        // slot invalido
                    }
                }
            }
        }
        return slots;
    }

    /** Configuracion de paginacion de un menu (para ciclos largos). */
    public int paginacion(String menuId, String clave, int porDefecto) {
        return archivo == null ? porDefecto : archivo.getInt(menuId + ".paginacion." + clave, porDefecto);
    }

    /** Nombres de los menus definidos en el archivo (menu-XXX). */
    public List<String> menus() {
        List<String> nombres = new ArrayList<String>();
        if (archivo == null) {
            return nombres;
        }
        for (String clave : archivo.getKeys(false)) {
            if (clave.startsWith("menu-") && archivo.isConfigurationSection(clave)) {
                nombres.add(clave);
            }
        }
        return nombres;
    }

    /** true si el menu se abre desde el comando (los que empiezan con menu-). */
    public boolean esMenu(String menuId) {
        return existe(menuId);
    }
}
