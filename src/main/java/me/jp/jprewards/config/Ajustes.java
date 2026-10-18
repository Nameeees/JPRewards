package me.jp.jprewards.config;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.storage.AjustesSql;
import me.jp.jprewards.storage.TipoAlmacen;
import me.jp.jprewards.util.Tiempo;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Lectura tipada de config.yml.
 *
 * Todo valor se VALIDA al cargarlo: si algo esta fuera de rango se corrige,
 * se explica por consola que se esperaba y queda anotado en {@link #problemas()}
 * para que /rewards info y /rewards validate lo puedan mostrar.
 * La idea es que un error de configuracion nunca rompa el plugin ni obligue
 * a leer un stacktrace.
 */
public final class Ajustes {

    private final JPRewards plugin;
    private final List<String> problemas = new ArrayList<String>();
    private FileConfiguration config;

    private int version;
    private String idioma;
    private boolean debug;

    private boolean moduloDiaria;
    private boolean moduloSemanal;
    private boolean moduloMensual;
    private boolean moduloRachas;

    private int horaReinicio;

    private String archivoDiarias;
    private int diasCiclo;
    private boolean perderRachaAlFaltar;

    private String archivoSemanales;
    private int semanasCiclo;
    private int diaInicioSemana;
    private boolean perderRachaSemanal;

    private String archivoMensuales;
    private String archivoRachas;

    private boolean avisarInventarioLleno;
    private boolean guardarPendientes;
    private boolean darExperienciaDirecta;

    private boolean avisoDiaria;
    private boolean avisoSemanal;
    private boolean avisoHitos;
    private boolean avisoRachas;
    private int avisoRetraso;
    private String avisoTipo;

    private boolean autoReclamar;
    private int autoReclamarRetraso;
    private boolean autoReclamarConPermiso;

    private String menusArchivo;
    private boolean gradienteTitulos;
    private final List<String> gradienteColores = new ArrayList<String>();
    private boolean cerrarAlReclamar;
    private boolean sonidoClicEnVacio;

    private boolean sonidosActivos;
    private float volumenSonido;
    private float tonoSonido;
    private String sonidoAbrir;
    private String sonidoClic;
    private String sonidoExito;
    private String sonidoError;
    private String sonidoAviso;

    private boolean titulosActivos;
    private boolean particulasActivas;
    private boolean fuegosActivos;
    private final List<String> fuegosColores = new ArrayList<String>();
    private int fuegosPotencia;

    private int barraLargo;
    private String barraLleno;
    private String barraVacio;
    private String barraColorLleno;
    private String barraColorVacio;

    private int topCantidad;

    private TipoAlmacen almacenTipo;
    private String almacenCarpeta;
    private String almacenModo;
    private int autoguardadoSegundos;
    private boolean guardarAlReclamar;
    private AjustesSql sql;

    private String moneda;

    private boolean actualizacionesActivas;
    private int recursoId;
    private boolean avisarAdminsActualizacion;

    public Ajustes(JPRewards plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Carga
    // ------------------------------------------------------------------

    /** Lee config.yml. Se llama en onEnable y en /rewards reload. */
    public void cargar() {
        problemas.clear();
        config = plugin.archivos().cargar("config.yml");
        if (config == null) {
            problemas.add("config.yml could not be read: using the default values.");
            plugin.getLogger().severe("config.yml could not be read; the defaults will be used.");
            config = plugin.archivos().cargarRecurso("config.yml");
            if (config == null) {
                config = new org.bukkit.configuration.file.YamlConfiguration();
            }
        }

        version = config.getInt("config-version", Archivos.CONFIG_VERSION);
        if (version > Archivos.CONFIG_VERSION) {
            problemas.add("config.yml comes from a newer version (" + version + "). Check the options.");
        }

        idioma = texto("idioma", "es");
        debug = config.getBoolean("debug", false);

        moduloDiaria = config.getBoolean("modulos.diaria", true);
        moduloSemanal = config.getBoolean("modulos.semanal", true);
        moduloMensual = config.getBoolean("modulos.mensual", true);
        moduloRachas = config.getBoolean("modulos.rachas", true);
        if (!moduloDiaria && !moduloSemanal && !moduloMensual && !moduloRachas) {
            problemas.add("All modules are disabled: the plugin will not deliver anything.");
        }

        horaReinicio = limitar(config.getInt("reinicio.hora", 0), 0, 23, "reinicio.hora", 0);

        archivoDiarias = texto("diaria.archivo", "recompensas/diarias.yml");
        diasCiclo = Math.max(0, config.getInt("diaria.dias-del-ciclo", 0));
        perderRachaAlFaltar = config.getBoolean("diaria.perder-racha-al-faltar", true);

        archivoSemanales = texto("semanal.archivo", "recompensas/semanales.yml");
        semanasCiclo = Math.max(0, config.getInt("semanal.semanas-del-ciclo", 0));
        String dia = texto("semanal.dia-inicio", "lunes");
        if (!Tiempo.diaValido(dia)) {
            avisar("semanal.dia-inicio", "valor '" + dia + "' is not a valid day (lunes/monday...domingo/sunday). Using lunes.");
            dia = "lunes";
        }
        diaInicioSemana = Tiempo.diaSemana(dia);
        perderRachaSemanal = config.getBoolean("semanal.perder-racha-al-faltar", true);

        archivoMensuales = texto("mensual.archivo", "recompensas/mensuales.yml");
        archivoRachas = texto("rachas.archivo", "recompensas/rachas.yml");

        avisarInventarioLleno = config.getBoolean("entrega.avisar-inventario-lleno", true);
        guardarPendientes = config.getBoolean("entrega.guardar-pendientes", true);
        darExperienciaDirecta = config.getBoolean("entrega.experiencia-directa", true);

        avisoDiaria = config.getBoolean("avisos.al-entrar.diaria", true);
        avisoSemanal = config.getBoolean("avisos.al-entrar.semanal", true);
        avisoHitos = config.getBoolean("avisos.al-entrar.hitos", true);
        avisoRachas = config.getBoolean("avisos.al-entrar.rachas", true);
        avisoRetraso = Math.max(0, config.getInt("avisos.al-entrar.retraso-segundos", 3));
        avisoTipo = texto("avisos.al-entrar.tipo", "actionbar").toLowerCase(java.util.Locale.ROOT);
        if (!"actionbar".equals(avisoTipo) && !"chat".equals(avisoTipo)) {
            avisar("avisos.al-entrar.tipo", "'" + avisoTipo + "' is not valid (actionbar or chat). Using actionbar.");
            avisoTipo = "actionbar";
        }

        autoReclamar = config.getBoolean("avisos.auto-reclamar.activado", false);
        autoReclamarRetraso = Math.max(0, config.getInt("avisos.auto-reclamar.retraso-segundos", 5));
        autoReclamarConPermiso = config.getBoolean("avisos.auto-reclamar.pedir-permiso", true);

        menusArchivo = texto("menus.archivo", "menus.yml");
        gradienteTitulos = config.getBoolean("menus.gradiente-titulos", true);
        gradienteColores.clear();
        gradienteColores.addAll(config.getStringList("menus.gradiente-colores"));
        if (gradienteColores.isEmpty()) {
            gradienteColores.add("&d");
            gradienteColores.add("&5");
        }
        cerrarAlReclamar = config.getBoolean("menus.cerrar-al-reclamar", false);
        sonidoClicEnVacio = config.getBoolean("menus.sonido-al-hacer-clic", true);

        sonidosActivos = config.getBoolean("sonidos.activado", true);
        volumenSonido = (float) limitarDecimal(config.getDouble("sonidos.volumen", 0.7D), 0.0D, 1.0D, "sonidos.volumen", 0.7D);
        tonoSonido = (float) limitarDecimal(config.getDouble("sonidos.tono", 1.4D), 0.5D, 2.0D, "sonidos.tono", 1.4D);
        sonidoAbrir = texto("sonidos.abrir-menu", "BLOCK_CHEST_OPEN");
        sonidoClic = texto("sonidos.clic", "UI_BUTTON_CLICK");
        sonidoExito = texto("sonidos.exito", "ENTITY_PLAYER_LEVELUP");
        sonidoError = texto("sonidos.error", "ENTITY_VILLAGER_NO");
        sonidoAviso = texto("sonidos.aviso", "ENTITY_EXPERIENCE_ORB_PICKUP");

        titulosActivos = config.getBoolean("efectos.titulos", true);
        particulasActivas = config.getBoolean("efectos.particulas", true);
        fuegosActivos = config.getBoolean("efectos.fuegos-artificiales", false);
        fuegosColores.clear();
        fuegosColores.addAll(config.getStringList("efectos.fuegos-colores"));
        if (fuegosColores.isEmpty()) {
            fuegosColores.add("AQUA");
            fuegosColores.add("FUCHSIA");
            fuegosColores.add("YELLOW");
        }
        fuegosPotencia = limitar(config.getInt("efectos.fuegos-potencia", 1), 0, 4, "efectos.fuegos-potencia", 1);

        barraLargo = limitar(config.getInt("barra-progreso.largo", 20), 3, 60, "barra-progreso.largo", 20);
        barraLleno = texto("barra-progreso.lleno", "|");
        barraVacio = texto("barra-progreso.vacio", "|");
        barraColorLleno = texto("barra-progreso.color-lleno", "&a");
        barraColorVacio = texto("barra-progreso.color-vacio", "&7");

        topCantidad = limitar(config.getInt("top.cantidad", 10), 1, 50, "top.cantidad", 10);

        String tipo = texto("almacenamiento.tipo", "yaml");
        TipoAlmacen leido = TipoAlmacen.desde(tipo);
        if (leido == null) {
            avisar("almacenamiento.tipo", "'" + tipo + "' is not valid (yaml or mysql). Using yaml.");
            leido = TipoAlmacen.YAML;
        }
        almacenTipo = leido;
        almacenCarpeta = texto("almacenamiento.carpeta", "datos");
        almacenModo = texto("almacenamiento.modo", "por-jugador").toLowerCase(java.util.Locale.ROOT);
        if (!"por-jugador".equals(almacenModo) && !"unico".equals(almacenModo)) {
            avisar("almacenamiento.modo", "'" + almacenModo + "' is not valid (por-jugador or unico). Using por-jugador.");
            almacenModo = "por-jugador";
        }
        autoguardadoSegundos = Math.max(0, config.getInt("almacenamiento.autoguardado-segundos", 300));
        guardarAlReclamar = config.getBoolean("almacenamiento.guardar-al-reclamar", true);
        sql = AjustesSql.leer(config);

        moneda = texto("economia.moneda", "monedas");

        actualizacionesActivas = config.getBoolean("actualizaciones.activado", false);
        recursoId = Math.max(0, config.getInt("actualizaciones.id-recurso", 0));
        avisarAdminsActualizacion = config.getBoolean("actualizaciones.avisar-admins", true);
        if (actualizacionesActivas && recursoId <= 0) {
            avisar("actualizaciones.id-recurso",
                    "to check for updates you need the SpigotMC resource id (0 = disabled).");
            actualizacionesActivas = false;
        }
    }

    // ------------------------------------------------------------------
    // Ayudas internas
    // ------------------------------------------------------------------

    private String texto(String ruta, String porDefecto) {
        String valor = config.getString(ruta, porDefecto);
        if (valor == null) {
            return porDefecto;
        }
        return valor;
    }

    private int limitar(int valor, int minimo, int maximo, String ruta, int porDefecto) {
        if (valor < minimo || valor > maximo) {
            avisar(ruta, "el valor " + valor + " esta fuera de rango (" + minimo + "-" + maximo + "). Using " + porDefecto + ".");
            return porDefecto;
        }
        return valor;
    }

    private double limitarDecimal(double valor, double minimo, double maximo, String ruta, double porDefecto) {
        if (valor < minimo || valor > maximo) {
            avisar(ruta, "el valor " + valor + " esta fuera de rango (" + minimo + "-" + maximo + "). Using " + porDefecto + ".");
            return porDefecto;
        }
        return valor;
    }

    /** Anota un problema de configuracion y lo avisa por consola. */
    public void avisar(String ruta, String mensaje) {
        problemas.add("config.yml -> " + ruta + ": " + mensaje);
        plugin.getLogger().warning("config.yml -> " + ruta + ": " + mensaje);
    }

    /** Problemas detectados en la ultima carga (los muestra /rewards info). */
    public List<String> problemas() {
        return new ArrayList<String>(problemas);
    }

    public FileConfiguration crudo() {
        return config;
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public int version() {
        return version;
    }

    public String idioma() {
        return idioma;
    }

    public boolean debug() {
        return debug;
    }

    public boolean moduloDiaria() {
        return moduloDiaria;
    }

    public boolean moduloSemanal() {
        return moduloSemanal;
    }

    public boolean moduloMensual() {
        return moduloMensual;
    }

    public boolean moduloRachas() {
        return moduloRachas;
    }

    public int horaReinicio() {
        return horaReinicio;
    }

    public String archivoDiarias() {
        return archivoDiarias;
    }

    public int diasCiclo() {
        return diasCiclo;
    }

    public boolean perderRachaAlFaltar() {
        return perderRachaAlFaltar;
    }

    public String archivoSemanales() {
        return archivoSemanales;
    }

    public int semanasCiclo() {
        return semanasCiclo;
    }

    public int diaInicioSemana() {
        return diaInicioSemana;
    }

    public boolean perderRachaSemanal() {
        return perderRachaSemanal;
    }

    public String archivoMensuales() {
        return archivoMensuales;
    }

    public String archivoRachas() {
        return archivoRachas;
    }

    public boolean avisarInventarioLleno() {
        return avisarInventarioLleno;
    }

    public boolean guardarPendientes() {
        return guardarPendientes;
    }

    public boolean darExperienciaDirecta() {
        return darExperienciaDirecta;
    }

    public boolean avisoDiaria() {
        return avisoDiaria;
    }

    public boolean avisoSemanal() {
        return avisoSemanal;
    }

    public boolean avisoHitos() {
        return avisoHitos;
    }

    public boolean avisoRachas() {
        return avisoRachas;
    }

    public int avisoRetraso() {
        return avisoRetraso;
    }

    public String avisoTipo() {
        return avisoTipo;
    }

    public boolean autoReclamar() {
        return autoReclamar;
    }

    public int autoReclamarRetraso() {
        return autoReclamarRetraso;
    }

    public boolean autoReclamarConPermiso() {
        return autoReclamarConPermiso;
    }

    public String menusArchivo() {
        return menusArchivo;
    }

    public boolean gradienteTitulos() {
        return gradienteTitulos;
    }

    public List<String> gradienteColores() {
        return new ArrayList<String>(gradienteColores);
    }

    public boolean cerrarAlReclamar() {
        return cerrarAlReclamar;
    }

    public boolean sonidoClicEnVacio() {
        return sonidoClicEnVacio;
    }

    public boolean sonidosActivos() {
        return sonidosActivos;
    }

    public float volumenSonido() {
        return volumenSonido;
    }

    public float tonoSonido() {
        return tonoSonido;
    }

    public String sonidoAbrir() {
        return sonidoAbrir;
    }

    public String sonidoClic() {
        return sonidoClic;
    }

    public String sonidoExito() {
        return sonidoExito;
    }

    public String sonidoError() {
        return sonidoError;
    }

    public String sonidoAviso() {
        return sonidoAviso;
    }

    public boolean titulosActivos() {
        return titulosActivos;
    }

    public boolean particulasActivas() {
        return particulasActivas;
    }

    public boolean fuegosActivos() {
        return fuegosActivos;
    }

    public List<String> fuegosColores() {
        return new ArrayList<String>(fuegosColores);
    }

    public int fuegosPotencia() {
        return fuegosPotencia;
    }

    public int barraLargo() {
        return barraLargo;
    }

    public String barraLleno() {
        return barraLleno;
    }

    public String barraVacio() {
        return barraVacio;
    }

    public String barraColorLleno() {
        return barraColorLleno;
    }

    public String barraColorVacio() {
        return barraColorVacio;
    }

    public int topCantidad() {
        return topCantidad;
    }

    public TipoAlmacen almacenTipo() {
        return almacenTipo;
    }

    public String almacenCarpeta() {
        return almacenCarpeta;
    }

    public String almacenModo() {
        return almacenModo;
    }

    public int autoguardadoSegundos() {
        return autoguardadoSegundos;
    }

    public boolean guardarAlReclamar() {
        return guardarAlReclamar;
    }

    public AjustesSql sql() {
        return sql;
    }

    /** Nombre de la moneda para el placeholder {moneda}. */
    public String moneda() {
        return moneda;
    }

    public boolean actualizacionesActivas() {
        return actualizacionesActivas;
    }

    public int recursoId() {
        return recursoId;
    }

    public boolean avisarAdminsActualizacion() {
        return avisarAdminsActualizacion;
    }
}
