package me.jp.jprewards.config;

import me.jp.jprewards.util.Items;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Definicion de un boton de menus.yml.
 *
 * Soporta variantes por estado: para una base "textura" busca
 * "textura-disponible", "textura-reclamada", etc. Asi un mismo boton cambia
 * de icono, nombre, lore, brillo y accion segun el estado del jugador sin
 * duplicar configuracion.
 *
 * Campos de item disponibles (todos opcionales salvo 'slot'):
 *   slot / slots, material, textura, textura-jugador, cantidad,
 *   nombre, lore, brillo, encantamientos, item-flags, custom-model-data,
 *   irrompible, icono-recompensa, permiso, ocultar-sin-permiso, accion.
 */
public final class ItemDef {

    private final ConfigurationSection seccion;
    private final int slot;

    public ItemDef(ConfigurationSection seccion, int slot) {
        this.seccion = seccion;
        this.slot = slot;
    }

    public int slot() {
        return slot;
    }

    public boolean existe() {
        return seccion != null;
    }

    /** Slots que ocupa el boton: 'slot: 20' o 'slots: [10, 11, 12]' o 'slots: 10-16'. */
    public List<Integer> slots(int maximo) {
        List<Integer> lista = new ArrayList<Integer>();
        if (seccion == null) {
            return lista;
        }
        if (seccion.isList("slots")) {
            for (Object valor : seccion.getList("slots")) {
                agregarSlot(lista, valor == null ? null : String.valueOf(valor), maximo);
            }
        } else if (seccion.isString("slots")) {
            for (String parte : seccion.getString("slots", "").split(",")) {
                agregarSlot(lista, parte, maximo);
            }
        }
        if (lista.isEmpty() && slot >= 0 && slot < maximo) {
            lista.add(slot);
        }
        return lista;
    }

    private void agregarSlot(List<Integer> lista, String valor, int maximo) {
        if (valor == null) {
            return;
        }
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            return;
        }
        if (limpio.contains("-")) {
            String[] partes = limpio.split("-");
            try {
                int desde = Integer.parseInt(partes[0].trim());
                int hasta = Integer.parseInt(partes[1].trim());
                for (int i = Math.min(desde, hasta); i <= Math.max(desde, hasta); i++) {
                    if (i >= 0 && i < maximo) {
                        lista.add(i);
                    }
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException error) {
                // rango invalido: se ignora
            }
            return;
        }
        try {
            int numero = Integer.parseInt(limpio);
            if (numero >= 0 && numero < maximo) {
                lista.add(numero);
            }
        } catch (NumberFormatException error) {
            // slot invalido: se ignora
        }
    }

    public String material() {
        return seccion == null ? "PLAYER_HEAD" : seccion.getString("material", "PLAYER_HEAD");
    }

    public String textura(String estado) {
        return estado("textura", estado, seccion == null ? "" : seccion.getString("textura", ""));
    }

    public String texturaJugador(String estado) {
        return estado("textura-jugador", estado, seccion == null ? "" : seccion.getString("textura-jugador", ""));
    }

    public int cantidad() {
        return seccion == null ? 1 : Math.max(1, seccion.getInt("cantidad", 1));
    }

    public int modelData() {
        return seccion == null ? 0 : Math.max(0, seccion.getInt("custom-model-data", 0));
    }

    public boolean irrompible() {
        return seccion != null && seccion.getBoolean("irrompible", false);
    }

    public Map<String, Integer> encantamientos() {
        return seccion == null ? new LinkedHashMap<String, Integer>()
                : Items.leerEncantamientos(seccion.getConfigurationSection("encantamientos"));
    }

    public List<String> flags() {
        return seccion == null ? new ArrayList<String>() : seccion.getStringList("item-flags");
    }

    /** Valor de una clave simple, o null. */
    public String variante(String clave) {
        return seccion == null ? null : seccion.getString(clave, null);
    }

    /** Valor de una clave simple con valor por defecto. */
    public String variante(String clave, String porDefecto) {
        return seccion == null ? porDefecto : seccion.getString(clave, porDefecto);
    }

    /** Busca <base>-<estado>; si no existe, devuelve la base o el valor por defecto. */
    public String estado(String base, String estado, String porDefecto) {
        if (seccion == null) {
            return porDefecto;
        }
        if (estado != null) {
            String valor = seccion.getString(base + "-" + estado, null);
            if (valor != null) {
                return valor;
            }
        }
        return seccion.getString(base, porDefecto);
    }

    /** Igual que estado() pero para listas. */
    public List<String> loreEstado(String base, String estado) {
        if (seccion == null) {
            return new ArrayList<String>();
        }
        if (estado != null && seccion.isSet(base + "-" + estado)) {
            return seccion.getStringList(base + "-" + estado);
        }
        return seccion.getStringList(base);
    }

    /** Lista de una clave concreta. */
    public List<String> lore(String clave) {
        return seccion == null ? new ArrayList<String>() : seccion.getStringList(clave);
    }

    public boolean booleano(String clave, boolean porDefecto) {
        return seccion != null && seccion.getBoolean(clave, porDefecto);
    }

    public boolean booleanoEstado(String base, String estado, boolean porDefecto) {
        if (seccion == null) {
            return porDefecto;
        }
        if (estado != null && seccion.contains(base + "-" + estado)) {
            return seccion.getBoolean(base + "-" + estado, porDefecto);
        }
        return seccion.getBoolean(base, porDefecto);
    }

    /** Acciones del boton: acepta una sola accion o una lista de acciones. */
    public List<String> acciones(String estado) {
        List<String> acciones = new ArrayList<String>();
        if (seccion == null) {
            return acciones;
        }
        String clave = "accion";
        if (estado != null && seccion.isSet("accion-" + estado)) {
            acciones.addAll(leerAcciones(seccion.get("accion-" + estado)));
            return acciones;
        }
        acciones.addAll(leerAcciones(seccion.get("accion")));
        return acciones;
    }

    private List<String> leerAcciones(Object valor) {
        List<String> acciones = new ArrayList<String>();
        if (valor == null) {
            return acciones;
        }
        if (valor instanceof List) {
            for (Object elemento : (List<?>) valor) {
                if (elemento != null && !String.valueOf(elemento).trim().isEmpty()) {
                    acciones.add(String.valueOf(elemento).trim());
                }
            }
        } else {
            String texto = String.valueOf(valor).trim();
            if (!texto.isEmpty()) {
                acciones.add(texto);
            }
        }
        return acciones;
    }

    /** Permiso que necesita el jugador para ver y usar el boton. */
    public String permiso() {
        return variante("permiso", "");
    }

    public boolean ocultarSinPermiso() {
        return booleano("ocultar-sin-permiso", false);
    }

    /** true si el estado 'hoy'/'disponible' debe usar la cabeza de la recompensa. */
    public boolean iconoRecompensa() {
        return booleano("icono-recompensa", false);
    }

    /** Construye el ItemStack del boton para un estado concreto. */
    public ItemStack crear(String estado, String nombre, List<String> lore, boolean brillo, ItemStack base) {
        ItemStack item = base;
        if (item == null) {
            item = Items.menu(material(), textura(estado), texturaJugador(estado), nombre, lore, brillo);
        } else {
            Items.aplicar(item, nombre, lore);
            if (brillo) {
                Items.brillo(item);
            }
        }
        if (item != null) {
            Items.ajustar(item, cantidad(), modelData(), encantamientos(), irrompible(), flags());
        }
        return item;
    }

    public ConfigurationSection seccion() {
        return seccion;
    }
}
