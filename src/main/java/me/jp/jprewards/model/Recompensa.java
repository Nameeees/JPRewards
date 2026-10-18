package me.jp.jprewards.model;

import me.jp.jprewards.util.Items;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Una recompensa leible desde YAML (un dia del ciclo, una semana, un hito
 * mensual o un bonus de racha) con VARIANTES POR RANGO.
 *
 * Estructura:
 *   - Los campos de arriba (icono/lore/comandos/items/efectos) son la
 *     version base: la que ve y recibe quien no tiene ninguno de los permisos
 *     de las variantes.
 *   - 'rangos' son variantes por permiso. Se usa la PRIMERA cuyo permiso
 *     tenga el jugador (en el orden del archivo: pon los rangos altos
 *     primero). Cada variante puede cambiar icono, lore, comandos, items,
 *     experiencia y efectos; lo que no defina se hereda de la base.
 *
 * La entrega puede ser por comandos (cualquier cosa que el servidor sepa
 * hacer) y/o por items directos y/o experiencia.
 */
public final class Recompensa {

    /** Una version de la recompensa: permiso + contenido + entrega. */
    public static final class Variante {

        private final String permiso;
        private final String nombre;
        private final ItemPremio icono;
        private final List<String> lore;
        private final List<String> comandos;
        private final List<ItemPremio> items;
        private final int experiencia;
        private final int niveles;
        private final EfectosPremio efectos;

        private Variante(String permiso, String nombre, ItemPremio icono, List<String> lore, List<String> comandos,
                List<ItemPremio> items, int experiencia, int niveles, EfectosPremio efectos) {
            this.permiso = permiso == null ? "" : permiso.trim();
            this.nombre = nombre;
            this.icono = icono;
            this.lore = lore;
            this.comandos = comandos;
            this.items = items;
            this.experiencia = experiencia;
            this.niveles = niveles;
            this.efectos = efectos == null ? EfectosPremio.vacio() : efectos;
        }

        public String permiso() {
            return permiso;
        }

        public String nombre() {
            return nombre;
        }

        /** Cabeza de esta variante (puede ser null: se usa la de la base). */
        public ItemStack icono() {
            return icono == null ? null : icono.crear();
        }

        public boolean tieneIcono() {
            return icono != null;
        }

        public List<String> lore() {
            return new ArrayList<String>(lore);
        }

        public List<String> comandos() {
            return new ArrayList<String>(comandos);
        }

        public List<ItemPremio> items() {
            return new ArrayList<ItemPremio>(items);
        }

        public int experiencia() {
            return experiencia;
        }

        public int niveles() {
            return niveles;
        }

        public EfectosPremio efectos() {
            return efectos;
        }

        /** true si no entrega nada (ni comandos, ni items, ni experiencia). */
        public boolean sinEntrega() {
            return comandos.isEmpty() && items.isEmpty() && experiencia <= 0 && niveles <= 0;
        }

        /** Completa los campos vacios con los de la variante base. */
        private Variante conBase(Variante base) {
            if (base == null) {
                return this;
            }
            ItemPremio iconoFinal = icono != null ? icono : base.icono;
            List<String> loreFinal = lore.isEmpty() ? base.lore : lore;
            List<String> comandosFinal = comandos.isEmpty() ? base.comandos : comandos;
            List<ItemPremio> itemsFinal = items.isEmpty() ? base.items : items;
            int experienciaFinal = experiencia > 0 ? experiencia : base.experiencia;
            int nivelesFinal = niveles > 0 ? niveles : base.niveles;
            String nombreFinal = nombre != null && !nombre.isEmpty() ? nombre : base.nombre;
            EfectosPremio efectosFinales = efectos.estaVacio() ? base.efectos : efectos;
            return new Variante(permiso, nombreFinal, iconoFinal, loreFinal, comandosFinal, itemsFinal,
                    experienciaFinal, nivelesFinal, efectosFinales);
        }
    }

    private final String id;
    private final String permiso;
    private final String nombrePorDefecto;
    private final Variante base;
    private final List<Variante> rangos;
    private final ConfigurationSection seccion;

    private Recompensa(String id, String permiso, String nombrePorDefecto, Variante base, List<Variante> rangos,
            ConfigurationSection seccion) {
        this.id = id;
        this.permiso = permiso == null ? "" : permiso.trim();
        this.nombrePorDefecto = nombrePorDefecto;
        this.base = base;
        this.rangos = rangos;
        this.seccion = seccion;
    }

    /** Lee una recompensa (y sus variantes de rango) desde YAML. */
    public static Recompensa leer(String id, ConfigurationSection seccion, String nombrePorDefecto) {
        if (seccion == null) {
            return null;
        }
        Variante baseLeida = leerVariante(seccion);
        // La base no lleva permiso propio: la exclusividad de la entrada entera
        // vive en 'permiso' y se comprueba aparte.
        Variante base = new Variante("", baseLeida.nombre, baseLeida.icono, baseLeida.lore, baseLeida.comandos,
                baseLeida.items, baseLeida.experiencia, baseLeida.niveles, baseLeida.efectos);

        List<Variante> rangos = new ArrayList<Variante>();
        ConfigurationSection seccionRangos = seccion.getConfigurationSection("rangos");
        if (seccionRangos != null) {
            for (String clave : seccionRangos.getKeys(false)) {
                ConfigurationSection seccionVariante = seccionRangos.getConfigurationSection(clave);
                if (seccionVariante == null) {
                    continue;
                }
                Variante variante = leerVariante(seccionVariante);
                if (variante.permiso().isEmpty()) {
                    org.bukkit.Bukkit.getLogger().warning("[JPRewards] " + id + " -> rangos." + clave
                            + " has no 'permiso' defined: that variant is skipped.");
                    continue;
                }
                rangos.add(variante);
            }
        }
        return new Recompensa(id, seccion.getString("permiso", ""), nombrePorDefecto, base, rangos, seccion);
    }

    private static Variante leerVariante(ConfigurationSection seccion) {
        ItemPremio icono = null;
        ConfigurationSection seccionIcono = seccion.getConfigurationSection("icono");
        if (seccionIcono != null) {
            String material = seccionIcono.getString("material", "");
            String textura = seccionIcono.getString("textura", "");
            String jugador = seccionIcono.getString("textura-jugador", "");
            if (!material.isEmpty() || !textura.isEmpty() || !jugador.isEmpty()) {
                icono = ItemPremio.leerUno(seccionIcono);
            }
        }

        List<String> lore = new ArrayList<String>();
        for (String linea : seccion.getStringList("lore")) {
            lore.add(String.valueOf(linea));
        }

        List<String> comandos = new ArrayList<String>();
        for (String comando : seccion.getStringList("comandos")) {
            if (comando != null && !comando.trim().isEmpty()) {
                comandos.add(comando.trim());
            }
        }

        return new Variante(seccion.getString("permiso", ""), seccion.getString("nombre", null), icono, lore, comandos,
                ItemPremio.leer(seccion, "items"), Math.max(0, seccion.getInt("experiencia", 0)),
                Math.max(0, seccion.getInt("niveles", 0)), EfectosPremio.leer(seccion));
    }

    public String id() {
        return id;
    }

    /** Permiso de exclusividad de la entrada completa ('' = para todos). */
    public String permiso() {
        return permiso;
    }

    public boolean requierePermiso() {
        return !permiso.isEmpty();
    }

    public boolean tieneVariantes() {
        return !rangos.isEmpty();
    }

    /** Variante que le corresponde a un jugador (la base si no tiene ningun rango). */
    public Variante variantePara(Player jugador) {
        if (jugador != null) {
            for (Variante variante : rangos) {
                if (jugador.hasPermission(variante.permiso())) {
                    return variante.conBase(base);
                }
            }
        }
        return base;
    }

    /** Variante base (sin jugador). */
    public Variante varianteBase() {
        return base;
    }

    /** Cabeza de la variante del jugador (puede ser null). */
    public ItemStack iconoPara(Player jugador) {
        Variante variante = variantePara(jugador);
        return variante == null ? null : variante.icono();
    }

    /** Nombre visible de la recompensa para un jugador. */
    public String nombrePara(Player jugador) {
        Variante variante = variantePara(jugador);
        String nombre = variante == null ? null : variante.nombre();
        if (nombre != null && !nombre.isEmpty()) {
            return nombre;
        }
        return nombrePorDefecto;
    }

    public String nombrePorDefecto() {
        return nombrePorDefecto;
    }

    /** Seccion YAML original (la usa el validador). */
    public ConfigurationSection seccion() {
        return seccion;
    }

    /** Cuantas variantes de rango definidas tiene la entrada. */
    public int numeroDeRangos() {
        return rangos.size();
    }

    /** true si la variante del jugador entrega algo. */
    public boolean entregaAlgoPara(Player jugador) {
        Variante variante = variantePara(jugador);
        return variante != null && !variante.sinEntrega();
    }

    /** Descripcion corta (para la vista previa y los comandos de prueba). */
    public static String describir(Variante variante) {
        if (variante == null || variante.sinEntrega()) {
            return "sin premios definidos";
        }
        List<String> partes = new ArrayList<String>();
        if (!variante.comandos().isEmpty()) {
            partes.add(variante.comandos().size() + " comando(s)");
        }
        if (!variante.items().isEmpty()) {
            StringBuilder items = new StringBuilder();
            for (ItemPremio premio : variante.items()) {
                if (items.length() > 0) {
                    items.append(", ");
                }
                items.append(premio.describir());
            }
            partes.add(items.toString());
        }
        if (variante.experiencia() > 0) {
            partes.add(variante.experiencia() + " xp");
        }
        if (variante.niveles() > 0) {
            partes.add(variante.niveles() + " nivel(es)");
        }
        return String.join(" + ", partes);
    }

    /** Utilidad para los menus: item del icono con nombre y lore aplicados. */
    public ItemStack iconoConTexto(Player jugador, String nombre, List<String> lore, boolean brillo) {
        ItemStack item = iconoPara(jugador);
        if (item == null) {
            return null;
        }
        Items.aplicar(item, nombre, lore);
        if (brillo) {
            Items.brillo(item);
        }
        return item;
    }
}
