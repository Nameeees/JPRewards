package me.jp.jprewards.menu;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.config.ItemDef;
import me.jp.jprewards.model.Recompensa;
import me.jp.jprewards.util.Items;
import me.jp.jprewards.util.Texto;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base de los menus de cofre del plugin.
 *
 *   - Cada menu se construye para UN jugador (los estados dependen de sus datos).
 *   - Cada slot puede llevar una o varias acciones; lo decorativo solo suena.
 *   - El marco, la navegacion (volver/cerrar) y los botones de pagina se leen
 *     de menus.yml, asi que se puede cambiar el aspecto sin tocar codigo.
 *   - Los ciclos largos se reparten en paginas automaticamente.
 */
public abstract class Menu implements InventoryHolder {

    protected final JPRewards plugin;
    protected final Player jugador;
    private final String menuId;
    private final Inventory inventario;
    private final Map<Integer, List<ClickAction>> acciones = new HashMap<Integer, List<ClickAction>>();

    /** Pagina actual (1..paginas). */
    protected int pagina = 1;

    /** Accion de un slot del menu. */
    public interface ClickAction {
        void ejecutar(InventoryClickEvent evento);
    }

    protected Menu(JPRewards plugin, Player jugador, String menuId) {
        this(plugin, jugador, menuId, 1);
    }

    protected Menu(JPRewards plugin, Player jugador, String menuId, int pagina) {
        this.plugin = plugin;
        this.jugador = jugador;
        this.menuId = menuId;
        this.pagina = Math.max(1, pagina);

        String titulo = plugin.menus().titulo(menuId);
        if (plugin.ajustes().gradienteTitulos()) {
            titulo = Texto.gradiente(titulo, plugin.ajustes().gradienteColores());
        }
        Map<String, String> ph = new java.util.LinkedHashMap<String, String>();
        ph.put("pagina", String.valueOf(this.pagina));
        ph.put("paginas", String.valueOf(paginas()));
        this.inventario = Bukkit.createInventory(this, plugin.menus().filas(menuId) * 9,
                Texto.limitar(Texto.color(Texto.reemplazar(titulo, ph)), 32));
    }

    public String menuId() {
        return menuId;
    }

    public Player jugador() {
        return jugador;
    }

    public int pagina() {
        return pagina;
    }

    /** Paginas totales del menu (1 por defecto; los ciclos largos la sobrescriben). */
    protected int paginas() {
        return 1;
    }

    // ------------------------------------------------------------------
    // Contenido
    // ------------------------------------------------------------------

    /** Define un item con acciones. */
    protected void set(int slot, ItemStack item, ClickAction accion) {
        if (item == null || slot < 0 || slot >= inventario.getSize()) {
            return;
        }
        inventario.setItem(slot, item);
        if (accion != null) {
            List<ClickAction> lista = acciones.get(Integer.valueOf(slot));
            if (lista == null) {
                lista = new ArrayList<ClickAction>();
                acciones.put(Integer.valueOf(slot), lista);
            }
            lista.add(accion);
        }
    }

    /** Define un item decorativo (solo suena al hacer clic). */
    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    /** Define varias acciones para el mismo slot. */
    protected void setAcciones(int slot, ItemStack item, List<ClickAction> lista) {
        if (item == null || slot < 0 || slot >= inventario.getSize()) {
            return;
        }
        inventario.setItem(slot, item);
        if (lista != null && !lista.isEmpty()) {
            acciones.put(Integer.valueOf(slot), new ArrayList<ClickAction>(lista));
        }
    }

    /**
     * Construye un item de menu: si 'base' no es null (por ejemplo el icono
     * propio de una recompensa) se usa como cabeza; si no, se crea una cabeza
     * con la textura configurada en menus.yml.
     */
    protected ItemStack item(ItemStack base, String material, String textura, String nombre, List<String> lore,
            boolean brillo) {
        if (base == null) {
            return Items.menu(material, textura, null, nombre, lore, brillo);
        }
        ItemStack item = base.clone();
        Items.aplicar(item, nombre, lore);
        if (brillo) {
            Items.brillo(item);
        }
        return item;
    }

    /** Cabeza de la recompensa para un estado concreto (segun el rango del jugador). */
    protected ItemStack iconoRecompensa(ItemDef def, String estado, Recompensa recompensa) {
        if (recompensa == null || def == null) {
            return null;
        }
        boolean estadoDePremio = "bloqueado".equals(estado) || "bloqueada".equals(estado)
                || (def.iconoRecompensa() && ("hoy".equals(estado) || "disponible".equals(estado)));
        return estadoDePremio ? recompensa.iconoPara(jugador) : null;
    }

    /**
     * Dibuja en un slot el boton de una recompensa con su estado
     * ('hoy', 'disponible', 'reclamado', 'bloqueado'...). Se encarga de todo:
     * permiso del boton, icono de tu variante de rango, nombre, lore con
     * {preview}, brillo y acciones.
     *
     * @return true si el boton se dibujo.
     */
    protected boolean pintarRecompensa(String claveBoton, int slot, Recompensa recompensa, String estado,
            Map<String, String> phBase, Map<String, String> phExtra, String accionPorDefecto) {
        ItemDef def = plugin.menus().boton(menuId, claveBoton);
        if (def == null) {
            return false;
        }
        if (!def.permiso().isEmpty() && !jugador.hasPermission(def.permiso()) && def.ocultarSinPermiso()) {
            return false;
        }
        Map<String, String> ph = new java.util.LinkedHashMap<String, String>(phBase);
        if (phExtra != null) {
            ph.putAll(phExtra);
        }
        ph.put("estado", estado);
        if (recompensa != null) {
            ph.put("premio", recompensa.nombrePara(jugador));
            ph.put("recompensa", recompensa.nombrePara(jugador));
        }
        List<String> preview = plugin.menuManager().preview(recompensa, jugador, ph);
        ItemStack base = iconoRecompensa(def, estado, recompensa);
        ItemStack item = item(base, def.material(), def.textura(estado),
                Texto.reemplazar(def.estado("nombre", estado, ""), ph),
                plugin.menuManager().lore(def, "lore", estado, ph, preview),
                def.booleanoEstado("brillo", estado, false));
        if (item == null) {
            return false;
        }
        String accion = def.variante("accion-" + estado, def.variante("accion", accionPorDefecto));
        // Un boton bloqueado que no hace nada responde con el sonido de error y
        // una pista en la action bar: se siente vivo en vez de "muerto".
        if (estado != null && estado.startsWith("bloqu")
                && (accion == null || accion.trim().isEmpty() || "none".equalsIgnoreCase(accion.trim())
                        || "nada".equalsIgnoreCase(accion.trim()))) {
            accion = "denied";
        }
        set(slot, item, plugin.menuManager().acciones(this, accion));
        return true;
    }

    /** Dibuja un boton "estatico" (tarjetas de informacion) con sus placeholders. */
    protected boolean pintarBoton(String claveBoton, Map<String, String> ph) {
        ItemDef def = plugin.menus().boton(menuId, claveBoton);
        if (def == null) {
            return false;
        }
        if (!def.permiso().isEmpty() && !jugador.hasPermission(def.permiso())) {
            if (def.ocultarSinPermiso()) {
                return false;
            }
        }
        List<Integer> slots = def.slots(inventario.getSize());
        if (slots.isEmpty()) {
            return false;
        }
        ItemStack item = def.crear(null, Texto.reemplazar(def.variante("nombre", ""), ph),
                Texto.reemplazar(def.lore("lore"), ph), def.booleano("brillo", false), null);
        if (item == null) {
            return false;
        }
        Menu.ClickAction accion = plugin.menuManager().acciones(this, def.variante("accion", "nada"));
        set(slots.get(0).intValue(), item, accion);
        return true;
    }

    /** Marco del cofre segun comun.marco (borde, todas o slots concretos). */
    protected void marco() {
        ItemDef marco = plugin.menus().comun("marco");
        if (marco == null) {
            return;
        }
        ItemStack item = marco.crear(null, marco.variante("nombre", " "), marco.lore("lore"), false, null);
        if (item == null) {
            return;
        }
        int filas = inventario.getSize() / 9;
        String modo = marco.variante("slots", "borde").toLowerCase(java.util.Locale.ROOT);
        for (int slot = 0; slot < inventario.getSize(); slot++) {
            if (inventario.getItem(slot) != null) {
                continue;
            }
            int fila = slot / 9;
            int columna = slot % 9;
            boolean esBorde = fila == 0 || fila == filas - 1 || columna == 0 || columna == 8;
            if ("todas".equals(modo) || ("borde".equals(modo) && esBorde)) {
                inventario.setItem(slot, item);
            }
        }
    }

    /** Coloca volver/cerrar segun menus.yml -> navegacion. */
    protected void navegacion() {
        int slotVolver = plugin.menus().navegacion(menuId, "volver-slot", 0);
        if (slotVolver > 0 && slotVolver < inventario.getSize()) {
            ItemDef volver = plugin.menus().comun("volver");
            if (volver != null) {
                ItemStack item = volver.crear(null, volver.variante("nombre", "&cVolver"), volver.lore("lore"), false, null);
                set(slotVolver, item, plugin.menuManager().acciones(this, volver.variante("accion", "abrir:principal")));
            }
        }
        int slotCerrar = plugin.menus().navegacion(menuId, "cerrar-slot", 0);
        if (slotCerrar > 0 && slotCerrar < inventario.getSize()) {
            ItemDef cerrar = plugin.menus().comun("cerrar");
            if (cerrar != null) {
                ItemStack item = cerrar.crear(null, cerrar.variante("nombre", "&cCerrar"), cerrar.lore("lore"), false, null);
                set(slotCerrar, item, plugin.menuManager().acciones(this, cerrar.variante("accion", "cerrar")));
            }
        }
    }

    /** Botones de pagina (solo si el menu tiene mas de una pagina). */
    protected void paginacion() {
        int total = paginas();
        if (total <= 1) {
            return;
        }
        Map<String, String> ph = new java.util.LinkedHashMap<String, String>();
        ph.put("pagina", String.valueOf(pagina));
        ph.put("paginas", String.valueOf(total));

        if (pagina > 1) {
            int slot = plugin.menus().paginacion(menuId, "anterior-slot", 39);
            ItemDef def = plugin.menus().comun("pagina-anterior");
            if (def != null && slot > 0 && slot < inventario.getSize() && inventario.getItem(slot) == null) {
                ItemStack item = def.crear(null, Texto.reemplazar(def.variante("nombre", "&ePagina anterior"), ph),
                        Texto.reemplazar(def.lore("lore"), ph), false, null);
                set(slot, item, plugin.menuManager().accion(this, "pagina:" + (pagina - 1)));
            }
        }
        if (pagina < total) {
            int slot = plugin.menus().paginacion(menuId, "siguiente-slot", 41);
            ItemDef def = plugin.menus().comun("pagina-siguiente");
            if (def != null && slot > 0 && slot < inventario.getSize() && inventario.getItem(slot) == null) {
                ItemStack item = def.crear(null, Texto.reemplazar(def.variante("nombre", "&ePagina siguiente"), ph),
                        Texto.reemplazar(def.lore("lore"), ph), false, null);
                set(slot, item, plugin.menuManager().accion(this, "pagina:" + (pagina + 1)));
            }
        }
        int slotInfo = plugin.menus().paginacion(menuId, "info-slot", 40);
        ItemDef def = plugin.menus().comun("pagina-info");
        if (def != null && slotInfo > 0 && slotInfo < inventario.getSize() && inventario.getItem(slotInfo) == null) {
            ItemStack item = def.crear(null, Texto.reemplazar(def.variante("nombre", "&7Pagina &f{pagina}&7/&f{paginas}"), ph),
                    Texto.reemplazar(def.lore("lore"), ph), false, null);
            set(slotInfo, item);
        }
    }

    /** Vacia el cofre y las acciones (para redibujar). */
    protected void limpiar() {
        inventario.clear();
        acciones.clear();
    }

    // ------------------------------------------------------------------
    // Reparto de items en el cofre
    // ------------------------------------------------------------------

    /** Filas interiores donde se reparten los items (fila central primero). */
    private static final int[][] FILAS_CENTRADAS = {
            {19, 20, 21, 22, 23, 24, 25},
            {10, 11, 12, 13, 14, 15, 16},
            {28, 29, 30, 31, 32, 33, 34}
    };

    /** Maximo de items que caben centrados en un cofre de 5 filas. */
    protected static final int MAXIMO_CENTRADOS = 21;

    /** Reparte N items centrados (fila central, luego arriba, luego abajo). */
    protected List<Integer> slotsCentrados(int cantidad) {
        List<Integer> slots = new ArrayList<Integer>();
        int restantes = cantidad;
        for (int[] fila : FILAS_CENTRADAS) {
            if (restantes <= 0) {
                break;
            }
            int enEstaFila = Math.min(restantes, fila.length);
            int inicio = (fila.length - enEstaFila) / 2;
            for (int i = 0; i < enEstaFila; i++) {
                slots.add(Integer.valueOf(fila[inicio + i]));
            }
            restantes -= enEstaFila;
        }
        return slots;
    }

    /**
     * Reparte N items en filas de 7 columnas empezando en 'slotInicial'
     * (asi slot-inicial: 10 llena la fila 2, luego la 3...).
     */
    protected List<Integer> slotsLineales(int slotInicial, int cantidad) {
        List<Integer> slots = new ArrayList<Integer>();
        if (slotInicial < 0) {
            return slots;
        }
        int fila = slotInicial / 9;
        int columna = slotInicial % 9;
        if (columna == 0) {
            columna = 1;
        }
        int restantes = cantidad;
        int slot = fila * 9 + columna;
        while (restantes > 0 && slot < inventario.getSize() - 9) {
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                continue;
            }
            slots.add(Integer.valueOf(slot));
            restantes--;
            slot++;
        }
        return slots;
    }

    /** Slots disponibles para items automaticos, segun la configuracion del menu. */
    protected List<Integer> slotsAutomaticos(int cantidad) {
        List<Integer> definidos = plugin.menus().slotsAutomaticos(menuId);
        if (!definidos.isEmpty()) {
            return definidos;
        }
        int inicial = plugin.menus().slotInicial(menuId, -1);
        if (inicial >= 0) {
            return slotsLineales(inicial, cantidad);
        }
        return slotsCentrados(cantidad);
    }

    /** Capacidad de una pagina (cuantos items automaticos entran). */
    protected int capacidadPagina() {
        List<Integer> definidos = plugin.menus().slotsAutomaticos(menuId);
        if (!definidos.isEmpty()) {
            return definidos.size();
        }
        int inicial = plugin.menus().slotInicial(menuId, -1);
        if (inicial >= 0) {
            return slotsLineales(inicial, 999).size();
        }
        return MAXIMO_CENTRADOS;
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    /** Reconstruye el menu en su sitio (tras reclamar o cambiar de pagina). */
    public void redibujar() {
        limpiar();
        construir();
        jugador.updateInventory();
    }

    /** Construye el contenido. Se llama al abrir y al redibujar. */
    protected abstract void construir();

    /** Abre el menu para su jugador. */
    public void abrir() {
        construir();
        jugador.openInventory(inventario);
        plugin.sonidos().abrir(jugador);
    }

    public void clic(InventoryClickEvent evento) {
        List<ClickAction> lista = acciones.get(Integer.valueOf(evento.getSlot()));
        if (lista == null) {
            return;
        }
        for (ClickAction accion : new ArrayList<ClickAction>(lista)) {
            try {
                accion.ejecutar(evento);
            } catch (Throwable error) {
                plugin.getLogger().warning("Error while running a menu action " + menuId + ": " + error.getMessage());
            }
        }
    }

    public boolean tieneAccion(int slot) {
        return acciones.containsKey(Integer.valueOf(slot));
    }

    @Override
    public Inventory getInventory() {
        return inventario;
    }
}
