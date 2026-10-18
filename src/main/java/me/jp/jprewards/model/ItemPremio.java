package me.jp.jprewards.model;

import me.jp.jprewards.util.Compat;
import me.jp.jprewards.util.Cabezas;
import me.jp.jprewards.util.Items;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Un premio que se entrega como ITEM (sin pasar por comandos).
 *
 * Formas de escribirlo en recompensas/*.yml:
 *
 *   items:
 *     - 'DIAMOND 3'                     # corto: material y cantidad
 *     - 'GOLDEN_APPLE:5'
 *     - material: DIAMOND_SWORD         # completo
 *       cantidad: 1
 *       nombre: '&bEspada del dia 7'
 *       lore:
 *         - '&7Premio de racha'
 *       encantamientos:
 *         SHARPNESS: 3
 *       item-flags: [HIDE_ATTRIBUTES]
 *       custom-model-data: 0
 *       irrompible: true
 *     - textura: 'eyJ0ZXh0dXJlcyI6...'  # cabeza con textura
 *       cantidad: 1
 *     - textura-jugador: 'Notch'        # cabeza de un jugador
 */
public final class ItemPremio {

    private final String material;
    private final String textura;
    private final String texturaJugador;
    private final int cantidad;
    private final String nombre;
    private final List<String> lore;
    private final Map<String, Integer> encantamientos;
    private final List<String> flags;
    private final int modelData;
    private final boolean irrompible;

    private ItemPremio(String material, String textura, String texturaJugador, int cantidad, String nombre, List<String> lore,
            Map<String, Integer> encantamientos, List<String> flags, int modelData, boolean irrompible) {
        this.material = material;
        this.textura = textura;
        this.texturaJugador = texturaJugador;
        this.cantidad = Math.max(1, cantidad);
        this.nombre = nombre;
        this.lore = lore;
        this.encantamientos = encantamientos;
        this.flags = flags;
        this.modelData = modelData;
        this.irrompible = irrompible;
    }

    /** Lee una lista de premios ("items:" de una recompensa). */
    public static List<ItemPremio> leer(ConfigurationSection seccion, String clave) {
        List<ItemPremio> premios = new ArrayList<ItemPremio>();
        if (seccion == null || !seccion.isSet(clave)) {
            return premios;
        }
        if (seccion.isList(clave)) {
            for (Object elemento : seccion.getList(clave)) {
                ItemPremio premio = leerElemento(elemento);
                if (premio != null) {
                    premios.add(premio);
                }
            }
            return premios;
        }
        // Formato mapa: items: { espada: {...}, pan: {...} }
        if (seccion.isConfigurationSection(clave)) {
            ConfigurationSection hijos = seccion.getConfigurationSection(clave);
            for (String nombreHijo : hijos.getKeys(false)) {
                if (hijos.isConfigurationSection(nombreHijo)) {
                    ItemPremio premio = leerSeccion(hijos.getConfigurationSection(nombreHijo));
                    if (premio != null) {
                        premios.add(premio);
                    }
                }
            }
        }
        return premios;
    }

    private static ItemPremio leerElemento(Object elemento) {
        if (elemento == null) {
            return null;
        }
        if (elemento instanceof ConfigurationSection) {
            return leerSeccion((ConfigurationSection) elemento);
        }
        if (elemento instanceof Map) {
            return leerMapa((Map<?, ?>) elemento);
        }
        return leerCorto(String.valueOf(elemento));
    }

    /** Formato corto: 'DIAMOND 3' o 'DIAMOND:3' o 'DIAMOND'. */
    private static ItemPremio leerCorto(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        String material = limpio;
        int cantidad = 1;
        java.util.regex.Matcher separador = java.util.regex.Pattern.compile("^([A-Za-z0-9_]+)\\s*[:x ]\\s*(\\d{1,3})$").matcher(limpio);
        if (separador.matches()) {
            material = separador.group(1);
            try {
                cantidad = Integer.parseInt(separador.group(2));
            } catch (NumberFormatException ignorado) {
                cantidad = 1;
            }
        }
        return new ItemPremio(material, "", "", cantidad, null, new ArrayList<String>(),
                new LinkedHashMap<String, Integer>(), new ArrayList<String>(), 0, false);
    }

    /** Lee UN item desde su seccion (lo usa el icono de las recompensas). */
    public static ItemPremio leerUno(ConfigurationSection seccion) {
        return leerSeccion(seccion);
    }

    private static ItemPremio leerSeccion(ConfigurationSection seccion) {
        if (seccion == null) {
            return null;
        }
        String material = seccion.getString("material", "");
        String textura = seccion.getString("textura", "");
        String texturaJugador = seccion.getString("textura-jugador", "");
        if (material.isEmpty() && textura.isEmpty() && texturaJugador.isEmpty()) {
            material = "STONE";
        }
        return new ItemPremio(material, textura, texturaJugador, seccion.getInt("cantidad", 1),
                seccion.getString("nombre", null), seccion.getStringList("lore"),
                Items.leerEncantamientos(seccion.getConfigurationSection("encantamientos")),
                seccion.getStringList("item-flags"), seccion.getInt("custom-model-data", 0),
                seccion.getBoolean("irrompible", false));
    }

    private static ItemPremio leerMapa(Map<?, ?> mapa) {
        ConfigurationSection seccion = new org.bukkit.configuration.file.YamlConfiguration();
        for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
            seccion.set(String.valueOf(entrada.getKey()), entrada.getValue());
        }
        return leerSeccion(seccion);
    }

    /** Construye el ItemStack que se entrega. */
    public ItemStack crear() {
        ItemStack item;
        if (textura != null && !textura.trim().isEmpty()) {
            item = Cabezas.crear(textura);
        } else if (texturaJugador != null && !texturaJugador.trim().isEmpty()) {
            item = Cabezas.deJugador(texturaJugador);
        } else {
            item = Compat.material(material, cantidad);
        }
        if (item == null) {
            return null;
        }
        if (nombre != null || (lore != null && !lore.isEmpty())) {
            Items.aplicar(item, nombre, lore);
        }
        Items.ajustar(item, cantidad, modelData, encantamientos, irrompible, flags);
        return item;
    }

    /** Descripcion corta para /rewards test y los mensajes. */
    public String describir() {
        String base;
        if (textura != null && !textura.trim().isEmpty()) {
            base = "cabeza personalizada";
        } else if (texturaJugador != null && !texturaJugador.trim().isEmpty()) {
            base = "cabeza de " + texturaJugador;
        } else {
            base = material;
        }
        if (nombre != null && !nombre.isEmpty()) {
            base = base + " (" + me.jp.jprewards.util.Texto.plano(nombre) + ")";
        }
        return cantidad > 1 ? cantidad + "x " + base : base;
    }

    public String material() {
        return material;
    }

    public int cantidad() {
        return cantidad;
    }
}
