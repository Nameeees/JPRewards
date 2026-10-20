package me.jp.testing;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.List;

/**
 * Harness de pruebas de JPRewards (plugin SOLO para servidores de prueba).
 *
 * Al habilitarse espera 3 segundos (para que JPRewards termine de cargar),
 * ejecuta las comprobaciones, imprime el resumen y apaga el servidor con
 * Bukkit.shutdown() para que la ejecucion sea desatendida:
 *
 *   CHECK OK|FAILED|SKIPPED: <texto>
 *   HARNESS RESULT checks=<n> failures=<n> skipped=<n>
 *   HARNESS PASS | HARNESS FAIL
 *
 * Nada de lo que haga el plugin bajo prueba puede impedir el apagado: todas
 * las comprobaciones van envueltas y el resumen se imprime siempre.
 */
public final class HarnessPlugin extends JavaPlugin {

    /** Espera antes de comprobar (3 s): deja que todos los plugins carguen. */
    private static final long ESPERA_ARRANQUE_TICKS = 60L;

    /** Duracion de la comprobacion de estabilidad (3 s). */
    private static final long ESPERA_ESTABILIDAD_TICKS = 60L;

    private final Registro registro = new Registro(System.out);
    private final CapturaLog captura = new CapturaLog();

    private Chequeos chequeos;
    private BukkitTask latido;
    private int latidos;

    @Override
    public void onEnable() {
        captura.instalar();
        registro.info("harness de JPRewards " + getDescription().getVersion());
        registro.info("servidor=" + Bukkit.getVersion());
        registro.info("bukkit=" + Bukkit.getBukkitVersion());
        registro.info("java=" + System.getProperty("java.version") + " de " + System.getProperty("java.vendor"));
        registro.info("carpeta de trabajo=" + new File(".").getAbsolutePath());
        try {
            if (!Bukkit.getWorlds().isEmpty()) {
                org.bukkit.World mundo = Bukkit.getWorlds().get(0);
                registro.info("mundo=" + mundo.getName() + " tipo=" + mundo.getWorldType()
                        + " altura-en-0,0=" + mundo.getHighestBlockYAt(0, 0) + " (en un mundo FLAT ronda 4)");
            }
        } catch (Throwable error) {
            registro.info("[harness] no se pudo inspeccionar el mundo: " + Registro.describir(error));
        }
        chequeos = new Chequeos(this, registro, captura);
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                comprobaciones();
            }
        }, ESPERA_ARRANQUE_TICKS);
    }

    @Override
    public void onDisable() {
        captura.quitar();
    }

    private void comprobaciones() {
        registro.info("[harness] ejecutando las comprobaciones");
        try {
            chequeos.pluginYComandos();
            chequeos.archivosPorDefecto();
            chequeos.yamlLegibles();
            chequeos.versionesConfig();
            chequeos.comandos();
            ChequeosDatos datos = new ChequeosDatos(chequeos, registro);
            datos.codecIdaYVuelta();
            datos.almacenYamlIdaYVuelta();
            ChequeosConfig configuracion = new ChequeosConfig(chequeos, registro, getLogger());
            configuracion.fusionConfiguracion();
            configuracion.fusionNoReescribeAlDia();
            configuracion.fusionRespetaListas();
            chequeos.mensajes();
        } catch (Throwable error) {
            registro.fallo("el harness no pudo completar las comprobaciones: " + Registro.describir(error));
        }
        estabilidad();
    }

    /** Comprobacion 9: una tarea repetida durante 3 s y todo sigue en pie. */
    private void estabilidad() {
        boolean activo = chequeos != null && chequeos.activo();
        if (activo) {
            captura.limpiar();
            captura.armar();
        }
        latidos = 0;
        latido = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                latidos++;
            }
        }, 1L, 20L);
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                terminar();
            }
        }, ESPERA_ESTABILIDAD_TICKS);
    }

    private void terminar() {
        if (latido != null) {
            latido.cancel();
            latido = null;
        }
        List<String> sucesos = captura.sucesos();
        captura.desarmar();
        Plugin jpr = chequeos == null ? null : chequeos.jprewards();
        boolean activo = jpr != null && jpr.isEnabled();
        int esperados = (int) (ESPERA_ESTABILIDAD_TICKS / 20L);
        if (!activo) {
            registro.saltado("estabilidad del scheduler: JPRewards no esta cargado y activo (latidos propios="
                    + latidos + ", errores en el log=" + sucesos.size() + ")");
        } else if (latidos < esperados - 1) {
            registro.fallo("estabilidad del scheduler: la tarea repetida se ejecuto " + latidos + " de " + esperados
                    + " veces (el scheduler no responde)");
        } else if (!sucesos.isEmpty()) {
            registro.fallo("estabilidad del scheduler: hubo errores en el log durante los 3 s -> " + sucesos.get(0));
        } else {
            registro.ok("estabilidad del scheduler: tarea repetida durante 3 s (" + latidos
                    + " latidos), JPRewards sigue activo y sin excepciones en el log");
        }
        registro.info("[harness] apagando el servidor (Bukkit.shutdown)");
        registro.imprimirResumen();
        try {
            Bukkit.shutdown();
        } catch (Throwable error) {
            registro.info("[harness] Bukkit.shutdown fallo: " + Registro.describir(error));
        }
    }
}
