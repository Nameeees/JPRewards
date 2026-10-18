package me.jp.jprewards.menu;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.config.ItemDef;
import me.jp.jprewards.config.Mensajes;
import me.jp.jprewards.model.BonusRacha;
import me.jp.jprewards.model.DatosJugador;
import me.jp.jprewards.model.Hito;
import me.jp.jprewards.model.Recompensa;
import me.jp.jprewards.util.Compat;
import me.jp.jprewards.util.Tiempo;
import me.jp.jprewards.util.Texto;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor de menus: apertura por id, traduccion de las acciones de menus.yml a
 * codigo, proteccion de los clics y placeholders compartidos.
 *
 * Los menus conocidos son principal, diario, semanal, mensual, rachas e info;
 * cualquier otro id que exista en menus.yml se puede abrir igual (por ejemplo
 * desde 'abrir:mi-menu'), aunque sin contenido automatico.
 */
public final class MenuManager implements Listener {

    private final JPRewards plugin;

    public MenuManager(JPRewards plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Apertura
    // ------------------------------------------------------------------

    /** Abre un menu por id. */
    public void abrir(Player jugador, String menuId) {
        abrir(jugador, menuId, 1);
    }

    /** Abre un menu por id en una pagina concreta. */
    public void abrir(Player jugador, String menuId, int pagina) {
        if (menuId == null || menuId.trim().isEmpty()) {
            menuId = "menu-principal";
        }
        String id = normalizarId(menuId.trim());
        if (!plugin.menus().existe(id)) {
            plugin.getLogger().warning("There is no '" + id + "' en " + plugin.ajustes().menusArchivo() + ".");
            return;
        }
        Menu menu;
        if ("menu-principal".equals(id)) {
            menu = new MenuPrincipal(plugin, jugador);
        } else if ("menu-diario".equals(id)) {
            menu = new MenuDiario(plugin, jugador, pagina);
        } else if ("menu-semanal".equals(id)) {
            menu = new MenuSemanal(plugin, jugador, pagina);
        } else if ("menu-mensual".equals(id)) {
            menu = new MenuMensual(plugin, jugador, pagina);
        } else if ("menu-rachas".equals(id)) {
            menu = new MenuRachas(plugin, jugador, pagina);
        } else if ("menu-info".equals(id)) {
            menu = new MenuInfo(plugin, jugador);
        } else {
            menu = new MenuGenerico(plugin, jugador, id);
        }
        menu.abrir();
    }

    /**
     * Acepta los nombres cortos que usaba la version 1.x ('principal',
     * 'diario', 'mensual', 'info'...) ademas de los ids completos de menus.yml
     * ('menu-principal', 'menu-diario'...). Asi los menus de una instalacion
     * antigua siguen funcionando sin tocarlos.
     */
    public String normalizarId(String menuId) {
        if (menuId == null || menuId.trim().isEmpty()) {
            return "menu-principal";
        }
        String id = menuId.trim();
        if (plugin.menus().existe(id) || id.startsWith("menu-")) {
            return id;
        }
        String conPrefijo = "menu-" + id;
        return plugin.menus().existe(conPrefijo) ? conPrefijo : id;
    }

    // ------------------------------------------------------------------
    // Acciones de menus.yml
    // ------------------------------------------------------------------

    /** Atajo de acciones() para una sola accion. */
    public Menu.ClickAction accion(final Menu menu, final String accion) {
        return acciones(menu, accion);
    }

    /** Traduce una accion (o varias separadas por ';') a codigo. */
    public Menu.ClickAction acciones(final Menu menu, final String accion) {
        final List<String> partes = new ArrayList<String>();
        if (accion != null) {
            for (String parte : accion.split(";")) {
                if (!parte.trim().isEmpty()) {
                    partes.add(parte.trim());
                }
            }
        }
        return new Menu.ClickAction() {
            @Override
            public void ejecutar(InventoryClickEvent evento) {
                for (String parte : partes) {
                    ejecutarUna(menu, parte);
                }
            }
        };
    }

    /** Ejecuta una accion concreta. */
    public void ejecutarUna(Menu menu, String accion) {
        if (accion == null || accion.trim().isEmpty()) {
            return;
        }
        String limpia = normalizarAccion(accion.trim());
        try {
            if (limpia.startsWith("open:")) {
                plugin.sonidos().clic(menu.jugador());
                abrir(menu.jugador(), limpia.substring("open:".length()).trim());
                return;
            }
            if (limpia.startsWith("page:")) {
                int destino = Integer.parseInt(limpia.substring("page:".length()).trim());
                plugin.sonidos().clic(menu.jugador());
                abrir(menu.jugador(), menu.menuId(), destino);
                return;
            }
            if ("close".equals(limpia)) {
                plugin.sonidos().clic(menu.jugador());
                menu.jugador().closeInventory();
                return;
            }
            // 'denied': el boton esta bloqueado; suena error y se explica por que.
            if ("denied".equals(limpia)) {
                plugin.sonidos().error(menu.jugador());
                String aviso = plugin.mensajes().texto("general.bloqueado", null, menu.jugador());
                if (!aviso.isEmpty()) {
                    Compat.actionBar(menu.jugador(), aviso);
                }
                return;
            }
            // 'none' (o 'nada'/'ninguna' en configuraciones antiguas): solo suena.
            if ("none".equals(limpia)) {
                if (plugin.ajustes().sonidoClicEnVacio()) {
                    plugin.sonidos().clic(menu.jugador());
                }
                return;
            }
            if (limpia.startsWith("claim:")) {
                reclamar(menu, limpia.substring("claim:".length()).trim());
                return;
            }
            if (limpia.startsWith("command:")) {
                String comando = limpia.substring("command:".length()).trim();
                comando = Texto.reemplazar(comando, placeholders(menu.jugador(), plugin.recompensas().datos(menu.jugador())));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);
                plugin.sonidos().clic(menu.jugador());
                return;
            }
            if (limpia.startsWith("player-command:")) {
                String comando = limpia.substring("player-command:".length()).trim();
                comando = Texto.reemplazar(comando, placeholders(menu.jugador(), plugin.recompensas().datos(menu.jugador())));
                menu.jugador().performCommand(comando);
                return;
            }
            if (limpia.startsWith("message:")) {
                plugin.mensajes().enviar(menu.jugador(), limpia.substring("message:".length()).trim());
                return;
            }
            if (limpia.startsWith("sound:")) {
                plugin.sonidos().reproducir(menu.jugador(), limpia.substring("sound:".length()).trim(), null, null);
                return;
            }
            plugin.sonidos().clic(menu.jugador());
        } catch (Throwable error) {
            plugin.getLogger().warning("The action '" + limpia + "' de " + menu.menuId() + " failed: " + error.getMessage());
        }
    }

    /**
     * Pasa una accion de menus.yml a su nombre canonico en ingles. Acepta
     * tambien los nombres que usaba la version 1.x (abrir, cerrar, nada,
     * reclamar, comando, mensaje, sonido...) para que los menus ya adaptados
     * sigan funcionando sin tocarlos.
     */
    public static String normalizarAccion(String accion) {
        if (accion == null) {
            return "";
        }
        String a = accion.trim();
        String minusculas = a.toLowerCase(java.util.Locale.ROOT);
        if (minusculas.startsWith("abrir:")) {
            return "open:" + a.substring("abrir:".length()).trim();
        }
        if (minusculas.startsWith("pagina:")) {
            return "page:" + a.substring("pagina:".length()).trim();
        }
        if ("cerrar".equals(minusculas) || "close".equals(minusculas)) {
            return "close";
        }
        if ("nada".equals(minusculas) || "ninguna".equals(minusculas) || "none".equals(minusculas)) {
            return "none";
        }
        if ("denied".equals(minusculas) || "bloqueado".equals(minusculas)) {
            return "denied";
        }
        if (minusculas.startsWith("reclamar:")) {
            return "claim:" + normalizarTipo(a.substring("reclamar:".length()).trim());
        }
        if (minusculas.startsWith("claim:")) {
            return "claim:" + normalizarTipo(a.substring("claim:".length()).trim());
        }
        if (minusculas.startsWith("comando:") || minusculas.startsWith("consola:")) {
            return "command:" + a.substring(a.indexOf(':') + 1).trim();
        }
        if (minusculas.startsWith("jugador-comando:")) {
            return "player-command:" + a.substring("jugador-comando:".length()).trim();
        }
        if (minusculas.startsWith("mensaje:")) {
            return "message:" + a.substring("mensaje:".length()).trim();
        }
        if (minusculas.startsWith("sonido:")) {
            return "sound:" + a.substring("sonido:".length()).trim();
        }
        return a;
    }

    /** Traduce el tipo de una accion 'claim' (acepta los nombres de la 1.x). */
    private static String normalizarTipo(String que) {
        String q = que.trim();
        String minusculas = q.toLowerCase(java.util.Locale.ROOT);
        if ("diaria".equals(minusculas) || "daily".equals(minusculas)) {
            return "daily";
        }
        if ("semanal".equals(minusculas) || "weekly".equals(minusculas)) {
            return "weekly";
        }
        if ("mensual".equals(minusculas) || "monthly".equals(minusculas) || "hitos".equals(minusculas)
                || "milestones".equals(minusculas)) {
            return "monthly";
        }
        if ("rachas".equals(minusculas) || "racha".equals(minusculas) || "bonus".equals(minusculas)
                || "streaks".equals(minusculas)) {
            return "streaks";
        }
        if ("todo".equals(minusculas) || "all".equals(minusculas)) {
            return "all";
        }
        if ("hito".equals(minusculas) || "milestone".equals(minusculas)) {
            return "monthly";
        }
        if ("racha".equals(minusculas) || "streak".equals(minusculas)) {
            return "streaks";
        }
        if (minusculas.startsWith("hito:")) {
            return "milestone:" + q.substring("hito:".length()).trim();
        }
        if (minusculas.startsWith("racha:")) {
            return "streak:" + q.substring("racha:".length()).trim();
        }
        return q;
    }

    private void reclamar(Menu menu, String que) {
        Player jugador = menu.jugador();
        DatosJugador datos = plugin.recompensas().datos(jugador);
        boolean entregado = false;
        if ("daily".equals(que)) {
            entregado = plugin.recompensas().reclamarDiaria(jugador);
        } else if ("weekly".equals(que)) {
            entregado = plugin.recompensas().reclamarSemanal(jugador);
        } else if ("monthly".equals(que)) {
            List<Hito> disponibles = plugin.recompensas().hitosDisponibles(datos);
            if (disponibles.isEmpty()) {
                plugin.mensajes().enviar(jugador, "mensual.nada-disponible");
                plugin.sonidos().error(jugador);
            }
            for (Hito hito : disponibles) {
                entregado = plugin.recompensas().reclamarHito(jugador, hito) || entregado;
            }
        } else if ("streaks".equals(que)) {
            List<BonusRacha> disponibles = plugin.recompensas().bonusDisponibles(datos);
            if (disponibles.isEmpty()) {
                plugin.mensajes().enviar(jugador, "rachas.nada-disponible");
                plugin.sonidos().error(jugador);
            }
            for (BonusRacha bono : disponibles) {
                entregado = plugin.recompensas().reclamarBonus(jugador, bono) || entregado;
            }
        } else if ("all".equals(que)) {
            entregado = plugin.recompensas().reclamarTodo(jugador) > 0;
        } else if (que.startsWith("milestone:")) {
            Hito hito = plugin.recompensas().hito(que.substring("milestone:".length()).trim());
            if (hito != null) {
                entregado = plugin.recompensas().reclamarHito(jugador, hito);
            }
        } else if (que.startsWith("streak:")) {
            BonusRacha bono = plugin.recompensas().bonus(que.substring("streak:".length()).trim());
            if (bono != null) {
                entregado = plugin.recompensas().reclamarBonus(jugador, bono);
            }
        }
        if (entregado) {
            menu.redibujar();
            if (plugin.ajustes().cerrarAlReclamar()) {
                jugador.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------------
    // Placeholders y vistas previas
    // ------------------------------------------------------------------

    /** Placeholders comunes a todos los menus. */
    public Map<String, String> placeholders(Player jugador, DatosJugador datos) {
        Map<String, String> ph = new LinkedHashMap<String, String>();
        List<Hito> hitosDisponibles = plugin.recompensas().hitosDisponibles(datos);
        List<BonusRacha> bonusDisponibles = plugin.recompensas().bonusDisponibles(datos);
        int diasDelMes = Tiempo.diasDelMesActual(plugin.ajustes().horaReinicio());

        ph.put("jugador", jugador.getName());
        ph.put("uuid", jugador.getUniqueId().toString());
        ph.put("mundo", jugador.getWorld() == null ? "" : jugador.getWorld().getName());
        ph.put("racha", String.valueOf(datos.racha()));
        ph.put("mejor", String.valueOf(datos.mejorRacha()));
        ph.put("total", String.valueOf(datos.total()));
        ph.put("dias_mes", String.valueOf(datos.diasMes()));
        ph.put("dias_mes_total", String.valueOf(diasDelMes));
        ph.put("tiempo", plugin.recompensas().tiempoRestante());
        ph.put("tiempo_largo", Tiempo.restanteLargo(plugin.ajustes().horaReinicio()));
        ph.put("hora", Tiempo.horaProximoCorte(plugin.ajustes().horaReinicio()));
        ph.put("moneda", plugin.ajustes().moneda());
        ph.put("hitos", String.valueOf(plugin.recompensas().hitosReclamados(datos)));
        ph.put("hitos_total", String.valueOf(plugin.recompensas().hitos().size()));
        ph.put("hitos_disponibles", String.valueOf(hitosDisponibles.size()));
        ph.put("pendientes", String.valueOf(hitosDisponibles.size()));
        ph.put("bonus", String.valueOf(plugin.recompensas().bonusReclamados(datos)));
        ph.put("bonus_total", String.valueOf(plugin.recompensas().bonus().size()));
        ph.put("bonus_disponibles", String.valueOf(bonusDisponibles.size()));
        ph.put("semana", String.valueOf(plugin.recompensas().proximaSemana(datos)));
        ph.put("semanas", String.valueOf(plugin.recompensas().semanasDelCiclo()));
        ph.put("dia", String.valueOf(plugin.recompensas().proximoDia(datos)));
        ph.put("dias", String.valueOf(plugin.recompensas().diasDelCiclo()));
        ph.put("racha_semanal", String.valueOf(datos.rachaSemanal()));
        ph.put("mejor_semanal", String.valueOf(datos.mejorRachaSemanal()));
        ph.put("total_semanal", String.valueOf(datos.totalSemanal()));
        ph.put("tiempo_semanal", plugin.recompensas().tiempoRestanteSemanal());
        ph.put("barra_mes", Texto.barra(datos.diasMes(), diasDelMes, plugin.ajustes().barraLargo(),
                plugin.ajustes().barraLleno(), plugin.ajustes().barraVacio(), plugin.ajustes().barraColorLleno(),
                plugin.ajustes().barraColorVacio()));
        ph.put("barra_ciclo", Texto.barra(plugin.recompensas().proximoDia(datos) - 1,
                plugin.recompensas().diasDelCiclo(), plugin.ajustes().barraLargo(), plugin.ajustes().barraLleno(),
                plugin.ajustes().barraVacio(), plugin.ajustes().barraColorLleno(), plugin.ajustes().barraColorVacio()));
        ph.put("barra_racha", Texto.barra(Math.min(datos.racha(), datos.mejorRacha() == 0 ? 1 : datos.mejorRacha()),
                datos.mejorRacha() == 0 ? 1 : datos.mejorRacha(), plugin.ajustes().barraLargo(),
                plugin.ajustes().barraLleno(), plugin.ajustes().barraVacio(), plugin.ajustes().barraColorLleno(),
                plugin.ajustes().barraColorVacio()));
        ph.put("barra_racha_semanal", Texto.barra(
                Math.min(datos.rachaSemanal(), datos.mejorRachaSemanal() == 0 ? 1 : datos.mejorRachaSemanal()),
                datos.mejorRachaSemanal() == 0 ? 1 : datos.mejorRachaSemanal(), plugin.ajustes().barraLargo(),
                plugin.ajustes().barraLleno(), plugin.ajustes().barraVacio(), plugin.ajustes().barraColorLleno(),
                plugin.ajustes().barraColorVacio()));
        ph.put("version", plugin.getDescription().getVersion());
        ph.put("servidor", me.jp.jprewards.util.Compat.versionServidor());
        ph.put("almacenamiento", plugin.datos().tipoReal().clave());
        ph.put("modulos", modulosActivos());
        return ph;
    }

    private String modulosActivos() {
        List<String> activos = new ArrayList<String>();
        if (plugin.ajustes().moduloDiaria()) {
            activos.add("diaria");
        }
        if (plugin.ajustes().moduloSemanal()) {
            activos.add("semanal");
        }
        if (plugin.ajustes().moduloMensual()) {
            activos.add("mensual");
        }
        if (plugin.ajustes().moduloRachas()) {
            activos.add("rachas");
        }
        return activos.isEmpty() ? "ninguno" : String.join(", ", activos);
    }

    /**
     * Lineas de lore de un boton con soporte de la marca {preview}: donde
     * aparezca, se insertan las lineas de la vista previa del premio.
     */
    public List<String> lore(ItemDef def, String base, String estado, Map<String, String> placeholders, List<String> preview) {
        List<String> resultado = new ArrayList<String>();
        for (String linea : def.loreEstado(base, estado)) {
            if (linea.contains("{preview}")) {
                resultado.addAll(preview);
            } else {
                resultado.add(Texto.reemplazar(linea, placeholders));
            }
        }
        return resultado;
    }

    /**
     * Vista previa de una recompensa: lore de la variante que le toca al
     * jugador segun su rango, mas avisos (sin permiso / mejora disponible).
     */
    public List<String> preview(Recompensa recompensa, Player jugador) {
        return preview(recompensa, jugador, placeholders(jugador, plugin.recompensas().datos(jugador)));
    }

    /**
     * Igual que preview() pero reutilizando los placeholders del menu. Los
     * lore de recompensas/*.yml admiten los MISMOS placeholders que menus.yml
     * (por ejemplo {moneda}, {jugador} o {racha}), asi que un mismo archivo de
     * premios se puede adaptar sin duplicarlo.
     */
    public List<String> preview(Recompensa recompensa, Player jugador, Map<String, String> ph) {
        List<String> lineas = new ArrayList<String>();
        String encabezado = plugin.menus().preview("encabezado");
        if (!encabezado.isEmpty()) {
            lineas.add(Texto.reemplazar(encabezado, ph));
        }
        Recompensa.Variante variante = recompensa == null ? null : recompensa.variantePara(jugador);
        if (variante == null || variante.lore().isEmpty()) {
            String vacia = plugin.menus().preview("vacia");
            if (!vacia.isEmpty()) {
                lineas.add(Texto.reemplazar(vacia, ph));
            }
        } else {
            lineas.addAll(Texto.reemplazar(variante.lore(), ph));
        }
        if (recompensa != null && recompensa.tieneVariantes() && (variante == null || variante.permiso().isEmpty())) {
            String pista = plugin.menus().preview("rango-mejor");
            if (!pista.isEmpty()) {
                lineas.add(Texto.reemplazar(pista, ph));
            }
        }
        if (recompensa != null && recompensa.requierePermiso() && !jugador.hasPermission(recompensa.permiso())) {
            String sinPermiso = plugin.menus().preview("sin-permiso");
            if (!sinPermiso.isEmpty()) {
                lineas.add(Texto.reemplazar(sinPermiso, ph));
            }
        }
        return lineas;
    }

    // ------------------------------------------------------------------
    // Proteccion de clics
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent evento) {
        InventoryHolder contenedor = evento.getInventory().getHolder();
        if (!(contenedor instanceof Menu)) {
            return;
        }
        Menu menu = (Menu) contenedor;
        evento.setCancelled(true);
        if (!(evento.getWhoClicked() instanceof Player)) {
            return;
        }
        if (evento.getRawSlot() < 0 || evento.getRawSlot() >= evento.getInventory().getSize()) {
            return;
        }
        if (!menu.jugador().getUniqueId().equals(evento.getWhoClicked().getUniqueId())) {
            return;
        }
        if (menu.tieneAccion(evento.getSlot())) {
            menu.clic(evento);
        } else if (plugin.ajustes().sonidoClicEnVacio()) {
            plugin.sonidos().clic(menu.jugador());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent evento) {
        if (evento.getInventory().getHolder() instanceof Menu) {
            evento.setCancelled(true);
        }
    }

    /** Mensajes: atajo para los menus. */
    public Mensajes mensajes() {
        return plugin.mensajes();
    }
}
