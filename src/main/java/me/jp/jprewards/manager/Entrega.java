package me.jp.jprewards.manager;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.config.Mensajes;
import me.jp.jprewards.model.DatosJugador;
import me.jp.jprewards.model.ItemPremio;
import me.jp.jprewards.model.Recompensa;
import me.jp.jprewards.util.Texto;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entrega de premios: comandos de consola, items directos, experiencia y
 * efectos. Es el unico sitio donde se le da algo a un jugador, asi que las
 * reglas son siempre las mismas:
 *
 *   - Los comandos se ejecutan desde CONSOLA con {jugador}, {uuid}, {mundo}
 *     y {premio} reemplazados.
 *   - Los items que no caben se guardan como pendientes (se entregan al
 *     volver a entrar) o se sueltan en el suelo, segun config.yml.
 *   - Un fallo en un premio NO cancela los demas.
 */
public final class Entrega {

    private final JPRewards plugin;

    public Entrega(JPRewards plugin) {
        this.plugin = plugin;
    }

    /** Resumen de lo entregado (lo usan los mensajes, el log y /rewards test). */
    public static final class Resultado {

        private int comandos;
        private int items;
        private int experiencia;
        private int niveles;
        private int pendientes;
        private int soltados;
        private final List<String> detalle = new ArrayList<String>();

        public int comandos() {
            return comandos;
        }

        public int items() {
            return items;
        }

        public int pendientes() {
            return pendientes;
        }

        public int soltados() {
            return soltados;
        }

        public List<String> detalle() {
            return new ArrayList<String>(detalle);
        }

        public boolean algoEntregado() {
            return comandos > 0 || items > 0 || experiencia > 0 || niveles > 0;
        }
    }

    /**
     * Entrega la recompensa al jugador (variante segun su rango) y devuelve el
     * resumen. Nunca lanza: los errores se avisan por consola.
     */
    public Resultado entregar(Player jugador, Recompensa recompensa, String nombreVisible) {
        Resultado resultado = new Resultado();
        if (jugador == null || recompensa == null) {
            return resultado;
        }
        Recompensa.Variante variante = recompensa.variantePara(jugador);

        Map<String, String> ph = new HashMap<String, String>();
        ph.put("jugador", jugador.getName());
        ph.put("uuid", jugador.getUniqueId().toString());
        ph.put("mundo", jugador.getWorld() == null ? "" : jugador.getWorld().getName());
        ph.put("premio", nombreVisible == null ? recompensa.nombrePara(jugador) : nombreVisible);

        // 1) Comandos de consola
        for (String comando : variante.comandos()) {
            String ejecutable = Texto.reemplazar(comando, ph).trim();
            if (ejecutable.isEmpty()) {
                continue;
            }
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ejecutable);
                resultado.comandos++;
                resultado.detalle.add("comando: " + ejecutable);
            } catch (Throwable error) {
                plugin.getLogger().warning("The command '" + ejecutable + "' of the reward " + recompensa.id()
                        + " failed: " + error.getMessage());
            }
        }

        // 2) Items directos
        for (ItemPremio premio : variante.items()) {
            ItemStack item = premio.crear();
            if (item == null) {
                plugin.getLogger().warning("Could not create un item of the reward " + recompensa.id()
                        + " (" + premio.describir() + ").");
                continue;
            }
            entregarItem(jugador, item, resultado);
        }

        // 3) Experiencia
        if (variante.experiencia() > 0 && plugin.ajustes().darExperienciaDirecta()) {
            jugador.giveExp(variante.experiencia());
            resultado.experiencia = variante.experiencia();
            resultado.detalle.add(variante.experiencia() + " puntos de experiencia");
        }
        if (variante.niveles() > 0 && plugin.ajustes().darExperienciaDirecta()) {
            jugador.giveExpLevels(variante.niveles());
            resultado.niveles = variante.niveles();
            resultado.detalle.add(variante.niveles() + " niveles de experiencia");
        }

        // 4) Efectos y mensajes (el aviso de inventario va una sola vez)
        avisarInventario(jugador, resultado);
        plugin.efectos().celebrar(jugador, variante.efectos(), ph.get("premio"), ph);
        return resultado;
    }

    /** Da un item, o lo guarda como pendiente, o lo suelta (nunca se pierde). */
    private void entregarItem(Player jugador, ItemStack item, Resultado resultado) {
        Map<Integer, ItemStack> sobrantes = jugador.getInventory().addItem(item);
        resultado.items++;
        resultado.detalle.add("item: " + me.jp.jprewards.util.Items.nombreVisible(item));
        if (sobrantes.isEmpty()) {
            return;
        }
        for (ItemStack sobrante : sobrantes.values()) {
            if (plugin.ajustes().guardarPendientes()) {
                DatosJugador datos = plugin.recompensas().datos(jugador);
                datos.agregarPendiente(sobrante);
                plugin.datos().guardar(datos);
                resultado.pendientes++;
            } else {
                jugador.getWorld().dropItemNaturally(jugador.getLocation(), sobrante);
                resultado.soltados++;
            }
        }
    }

    /** Aviso unico (y solo si hace falta) de que algo no cupo en el inventario. */
    private void avisarInventario(Player jugador, Resultado resultado) {
        if (resultado.pendientes() > 0 && plugin.ajustes().avisarInventarioLleno()) {
            plugin.mensajes().enviar(jugador, "general.inventario-lleno",
                    Mensajes.ph("cantidad", String.valueOf(resultado.pendientes())));
        } else if (resultado.soltados() > 0 && plugin.ajustes().avisarInventarioLleno()) {
            plugin.mensajes().enviar(jugador, "general.inventario-soltado",
                    Mensajes.ph("cantidad", String.valueOf(resultado.soltados())));
        }
    }

    /** Entrega los items que quedaron pendientes (al entrar al servidor). */
    public void entregarPendientes(Player jugador) {
        DatosJugador datos = plugin.recompensas().datos(jugador);
        if (!datos.tienePendientes()) {
            return;
        }
        List<ItemStack> pendientes = datos.tomarPendientes();
        List<ItemStack> noEntregados = new ArrayList<ItemStack>();
        for (ItemStack item : pendientes) {
            Map<Integer, ItemStack> sobrantes = jugador.getInventory().addItem(item);
            if (!sobrantes.isEmpty()) {
                noEntregados.addAll(sobrantes.values());
            }
        }
        for (ItemStack sobrante : noEntregados) {
            datos.agregarPendiente(sobrante);
        }
        int entregados = pendientes.size() - noEntregados.size();
        if (entregados > 0) {
            plugin.mensajes().enviar(jugador, "general.pendientes-recibidos",
                    Mensajes.ph("cantidad", String.valueOf(entregados)));
            plugin.sonidos().exito(jugador);
        }
        plugin.datos().guardar(datos);
    }
}
