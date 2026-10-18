package me.jp.jprewards.util;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Constructor de los items de menu y de los premios: cabezas con textura,
 * nombre, lore, cantidad, brillo, encantamientos, item-flags, model data e
 * irrompibilidad. Compatible de 1.8 a la ultima version.
 */
public final class Items {

    private Items() {
    }

    /**
     * Item de menu: si hay textura se crea una cabeza con ella; si hay
     * textura-jugador, la cabeza de ese jugador; si no, el material indicado.
     */
    public static ItemStack menu(String material, String textura, String texturaJugador, String nombre, List<String> lore, boolean brillo) {
        ItemStack item;
        if (textura != null && !textura.trim().isEmpty()) {
            item = Cabezas.crear(textura);
        } else if (texturaJugador != null && !texturaJugador.trim().isEmpty()) {
            item = Cabezas.deJugador(texturaJugador);
        } else {
            item = Compat.material(material, 1);
        }
        if (item == null) {
            return null;
        }
        aplicar(item, nombre, lore);
        if (brillo) {
            brillo(item);
        }
        return item;
    }

    /**
     * Aplica los ajustes opcionales de un item (todos se pueden dejar vacios).
     *
     * @param cantidad     cantidad del stack (1-64; se limita solo)
     * @param modelData    custom-model-data (1.14+; se ignora en versiones antiguas)
     * @param encantamientos nombre -> nivel (acepta nombres modernos y de 1.8)
     * @param irrompible   unbreakable (1.11+) o durabilidad oculta en 1.8
     * @param flags        item-flags (HIDE_ATTRIBUTES, HIDE_ENCHANTS...)
     */
    public static void ajustar(ItemStack item, int cantidad, int modelData, Map<String, Integer> encantamientos,
            boolean irrompible, List<String> flags) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return;
        }
        if (cantidad > 1) {
            item.setAmount(Math.min(item.getMaxStackSize(), cantidad));
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (encantamientos != null && !encantamientos.isEmpty()) {
            for (Map.Entry<String, Integer> entrada : encantamientos.entrySet()) {
                Enchantment encantamiento = Compat.encantamiento(entrada.getKey());
                if (encantamiento != null) {
                    meta.addEnchant(encantamiento, Math.max(1, entrada.getValue()), true);
                }
            }
        }
        if (flags != null && !flags.isEmpty()) {
            for (String flag : flags) {
                ItemFlag valor = Compat.flag(flag);
                if (valor != null) {
                    meta.addItemFlags(valor);
                }
            }
        }
        if (modelData > 0) {
            Compat.customModelData(meta, modelData);
        }
        if (irrompible) {
            Compat.irrompible(meta, true);
        }
        item.setItemMeta(meta);
    }

    /** Aplica nombre y lore coloreados a un item. */
    public static void aplicar(ItemStack item, String nombre, List<String> lore) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (nombre != null && !nombre.isEmpty()) {
            meta.setDisplayName(Texto.color(nombre));
        }
        if (lore != null && !lore.isEmpty()) {
            List<String> lineas = new ArrayList<String>();
            for (String linea : lore) {
                lineas.add(Texto.color(linea));
            }
            meta.setLore(lineas);
        }
        item.setItemMeta(meta);
    }

    /** Brillo sin encantamiento visible (funciona de 1.8 a la ultima version). */
    public static void brillo(ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return;
        }
        Enchantment encantamiento = Compat.encantamiento("UNBREAKING");
        if (encantamiento == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.addEnchant(encantamiento, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    /** Quita el brillo (por si un estado lo desactiva tras un estado que lo activa). */
    public static void quitarBrillo(ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        for (Map.Entry<Enchantment, Integer> entrada : new LinkedHashMap<Enchantment, Integer>(meta.getEnchants()).entrySet()) {
            meta.removeEnchant(entrada.getKey());
        }
        meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    /** Lee encantamientos de una seccion YAML (nombre: nivel). */
    public static Map<String, Integer> leerEncantamientos(org.bukkit.configuration.ConfigurationSection seccion) {
        Map<String, Integer> encantamientos = new LinkedHashMap<String, Integer>();
        if (seccion == null) {
            return encantamientos;
        }
        for (String clave : seccion.getKeys(false)) {
            encantamientos.put(clave, seccion.getInt(clave, 1));
        }
        return encantamientos;
    }

    /** Nombre legible de un item (para mensajes y logs). */
    public static String nombreVisible(ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return "-";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return Texto.plano(meta.getDisplayName());
        }
        return item.getType().name() + (item.getAmount() > 1 ? " x" + item.getAmount() : "");
    }
}
