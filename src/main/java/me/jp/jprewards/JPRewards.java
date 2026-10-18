package me.jp.jprewards;

import me.jp.jprewards.command.ComandoRecompensas;
import me.jp.jprewards.config.Ajustes;
import me.jp.jprewards.config.Archivos;
import me.jp.jprewards.config.Mensajes;
import me.jp.jprewards.config.MenuArchivo;
import me.jp.jprewards.config.Validador;
import me.jp.jprewards.hook.Actualizaciones;
import me.jp.jprewards.hook.PuentePlaceholders;
import me.jp.jprewards.listener.IngresoListener;
import me.jp.jprewards.manager.DatosManager;
import me.jp.jprewards.manager.Estadisticas;
import me.jp.jprewards.manager.Migrador;
import me.jp.jprewards.manager.RecompensaManager;
import me.jp.jprewards.menu.MenuManager;
import me.jp.jprewards.util.Cabezas;
import me.jp.jprewards.util.Efectos;
import me.jp.jprewards.util.Sonidos;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * JPRewards - recompensas diarias, semanales, mensuales y de racha con menus
 * de cabezas totalmente configurables.
 *
 * Una sola build para Minecraft 1.8 hasta la ultima version: se compila
 * contra la API de Spigot 1.8.8 y todo lo que cambia entre versiones
 * (materiales, sonidos, particulas, titulos) se resuelve en tiempo de
 * ejecucion. Sin NMS, sin dependencias y sin codigo ofuscado.
 *
 * Arranque ordenado y protegido: si algo falla al arrancar, el plugin lo
 * explica por consola y se desactiva limpiamente en vez de dejar el servidor
 * a medias de una NullPointerException.
 */
public final class JPRewards extends JavaPlugin {

    private Archivos archivos;
    private Ajustes ajustes;
    private Mensajes mensajes;
    private MenuArchivo menus;
    private Sonidos sonidos;
    private Efectos efectos;
    private DatosManager datos;
    private RecompensaManager recompensas;
    private MenuManager menuManager;
    private Validador validador;
    private Estadisticas estadisticas;
    private Migrador migrador;
    private Actualizaciones actualizaciones;

    @Override
    public void onEnable() {
        try {
            arrancar();
        } catch (Throwable error) {
            getLogger().severe("JPRewards could not start: " + error);
            error.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void arrancar() {
        // 1) Archivos: crear lo que falte y agregar las opciones nuevas sin
        //    tocar lo que el usuario ya configuro.
        archivos = new Archivos(getDataFolder(), getLogger());
        archivos.crearPorDefecto(this);
        actualizarArchivos();

        // 2) Configuracion
        ajustes = new Ajustes(this);
        ajustes.cargar();
        mensajes = new Mensajes(this);
        mensajes.cargar(ajustes.idioma());
        menus = new MenuArchivo(this);
        menus.cargar();
        sonidos = new Sonidos(this);
        efectos = new Efectos(this);

        // 3) Datos y recompensas
        datos = new DatosManager(this);
        datos.iniciar();
        recompensas = new RecompensaManager(this);
        recompensas.cargar();

        // 4) Menus, comandos y eventos
        menuManager = new MenuManager(this);
        getServer().getPluginManager().registerEvents(menuManager, this);
        getServer().getPluginManager().registerEvents(new IngresoListener(this), this);

        PluginCommand comando = getCommand("rewards");
        if (comando == null) {
            getLogger().severe("The 'rewards' command is not in plugin.yml: the plugin cannot work."
                    + " Reinstall the .jar (it is corrupt or from another version).");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        ComandoRecompensas ejecutor = new ComandoRecompensas(this);
        comando.setExecutor(ejecutor);
        comando.setTabCompleter(ejecutor);

        // 5) Servicios
        validador = new Validador(this);
        estadisticas = new Estadisticas(this);
        migrador = new Migrador(this);
        actualizaciones = new Actualizaciones(this);
        actualizaciones.comprobar();
        PuentePlaceholders.registrar(this);

        // 6) Resumen y primera revision
        revisarAlArrancar();
        mostrarBanner();
    }

    /**
     * Resumen de arranque: lo primero que ve un administrador, asi que dice
     * todo lo importante en cinco lineas y sin ruido.
     */
    private void mostrarBanner() {
        getLogger().info("-----------------------------------------------");
        getLogger().info(" JPRewards " + getDescription().getVersion()
                + "  |  " + Bukkit.getBukkitVersion().split("-")[0] + "  |  " + Bukkit.getName());
        if (recompensas.diasDelCiclo() > 0) {
            getLogger().info(" Rewards: " + recompensas.diasDelCiclo() + " daily days, "
                    + recompensas.semanasDelCiclo() + " weekly blocks, " + recompensas.hitos().size()
                    + " monthly milestones, " + recompensas.bonus().size() + " streak bonuses (language "
                    + mensajes.idioma() + ").");
        }
        getLogger().info(" Storage: " + datos.describir());
        getLogger().info(" " + estadoConfiguracion());
        if (archivos.primeraVez()) {
            getLogger().info(" First start: the default files were created. Edit config.yml, menus.yml and");
            getLogger().info(" recompensas/*.yml, then run '/rewards validate' and '/rewards reload'.");
        } else {
            getLogger().info(" Commands: /rewards (menu)  |  /rewards help  |  /rewards validate  |  /rewards reload");
        }
        getLogger().info("-----------------------------------------------");
    }

    /** Texto corto con el estado de la configuracion para el banner. */
    private String estadoConfiguracion() {
        try {
            Validador.Resultado resultado = validador.validar();
            if (resultado.errores().isEmpty() && resultado.avisos().isEmpty()) {
                return "Config: OK (" + archivos.gestionados().length + " files, no problems found)";
            }
            if (resultado.errores().isEmpty()) {
                return "Config: " + resultado.avisos().size() + " warning(s) - run '/rewards validate' for details";
            }
            return "Config: " + resultado.errores().size() + " error(s) - run '/rewards validate' for details";
        } catch (Throwable error) {
            return "Config: not checked (" + error.getMessage() + ")";
        }
    }

    /**
     * Revisa los archivos al arrancar y avisa de lo importante sin llenar la
     * consola: el detalle completo esta en /rewards validate.
     */
    private void revisarAlArrancar() {
        try {
            Validador.Resultado resultado = validador.validar();
            if (!resultado.errores().isEmpty()) {
                for (String error : resultado.errores().subList(0, Math.min(5, resultado.errores().size()))) {
                    getLogger().warning(" - " + error);
                }
            } else if (!resultado.avisos().isEmpty()) {
                for (String aviso : resultado.avisos().subList(0, Math.min(3, resultado.avisos().size()))) {
                    getLogger().info(" - " + aviso);
                }
            } else if (ajustes.debug()) {
                getLogger().info("Configuration checked: no errors and no warnings.");
            }
        } catch (Throwable error) {
            getLogger().warning("Could not check the configuration: " + error.getMessage());
        }
    }

    /**
     * Agrega a los archivos del usuario las opciones nuevas de esta version
     * (con sus comentarios) y sube config-version. Antes de tocar cada archivo
     * se guarda una copia .bak.
     */
    private void actualizarArchivos() {
        List<String> resumen = new ArrayList<String>();
        for (String ruta : Archivos.gestionados()) {
            if (!archivos.existe(ruta)) {
                continue;
            }
            try {
                List<String> nuevas = archivos.actualizar(ruta, Archivos.CONFIG_VERSION);
                if (!nuevas.isEmpty()) {
                    resumen.add(ruta + " (+" + nuevas.size() + ")");
                    if (ajustes != null && ajustes.debug()) {
                        for (String clave : nuevas) {
                            getLogger().info("Opción nueva en " + ruta + ": " + clave);
                        }
                    }
                }
            } catch (Throwable error) {
                getLogger().warning("Could not update " + ruta + ": " + error.getMessage());
            }
        }
        if (!resumen.isEmpty()) {
            getLogger().info("Files updated with the new options (a .bak copy was saved): "
                    + String.join(", ", resumen) + ". Check the comments marked as NEW.");
        }
    }

    @Override
    public void onDisable() {
        if (datos != null) {
            int perfiles = datos.enMemoria();
            datos.cerrar();
            getLogger().info("Disabled: " + perfiles + " player profile(s) saved and the data folder is safe to back up.");
        }
    }

    // ------------------------------------------------------------------
    // Recarga en caliente
    // ------------------------------------------------------------------

    /**
     * Recarga una parte del plugin.
     *
     * @param que all | config | messages | menus | rewards
     */
    public void recargar(String que) {
        String tipo = que == null ? "all" : que.toLowerCase(java.util.Locale.ROOT);
        if ("all".equals(tipo) || "config".equals(tipo)) {
            actualizarArchivos();
            ajustes.cargar();
            datos.reiniciarAutoguardado();
        }
        if ("all".equals(tipo) || "messages".equals(tipo)) {
            mensajes.cargar(ajustes.idioma());
        }
        if ("all".equals(tipo) || "menus".equals(tipo)) {
            menus.cargar();
            Cabezas.limpiarCache();
        }
        if ("all".equals(tipo) || "rewards".equals(tipo)) {
            recompensas.cargar();
        }
        // Los menus abiertos se cierran para que no queden con datos viejos.
        for (org.bukkit.entity.Player jugador : getServer().getOnlinePlayers()) {
            if (jugador.getOpenInventory() != null
                    && jugador.getOpenInventory().getTopInventory() != null
                    && jugador.getOpenInventory().getTopInventory().getHolder() instanceof me.jp.jprewards.menu.Menu) {
                jugador.closeInventory();
            }
        }
    }

    // ------------------------------------------------------------------
    // Accesores
    // ------------------------------------------------------------------

    public Archivos archivos() {
        return archivos;
    }

    public Ajustes ajustes() {
        return ajustes;
    }

    public Mensajes mensajes() {
        return mensajes;
    }

    public MenuArchivo menus() {
        return menus;
    }

    public Sonidos sonidos() {
        return sonidos;
    }

    public Efectos efectos() {
        return efectos;
    }

    public DatosManager datos() {
        return datos;
    }

    public RecompensaManager recompensas() {
        return recompensas;
    }

    public MenuManager menuManager() {
        return menuManager;
    }

    public Validador validador() {
        return validador;
    }

    public Estadisticas estadisticas() {
        return estadisticas;
    }

    public Migrador migrador() {
        return migrador;
    }

    public Actualizaciones hookActualizaciones() {
        return actualizaciones;
    }
}
