package me.jp.jprewards.hook;

import me.jp.jprewards.JPRewards;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Aviso de version nueva usando la API publica de SpigotMC.
 *
 * Esta DESACTIVADO por defecto (config.yml -> actualizaciones.activado) y solo
 * hace una peticion HTTPS al arrancar: nada de datos del servidor ni de los
 * jugadores, solo el numero de version mas reciente del recurso.
 */
public final class Actualizaciones {

    private static final String API = "https://api.spigotmc.org/legacy/update.php?resource=";

    private final JPRewards plugin;
    private String ultimaVersion;
    private boolean hayNueva;

    public Actualizaciones(JPRewards plugin) {
        this.plugin = plugin;
    }

    /** Comprueba la version en segundo plano (no bloquea el arranque). */
    public void comprobar() {
        if (!plugin.ajustes().actualizacionesActivas()) {
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                String version = consultar(plugin.ajustes().recursoId());
                if (version == null || version.trim().isEmpty()) {
                    return;
                }
                ultimaVersion = version.trim();
                hayNueva = comparar(ultimaVersion, plugin.getDescription().getVersion()) > 0;
                if (hayNueva) {
                    plugin.getLogger().warning("A new JPRewards version is available: " + ultimaVersion
                            + " (estas usando " + plugin.getDescription().getVersion()
                            + "). Descargala en https://www.spigotmc.org/resources/" + plugin.ajustes().recursoId());
                } else if (plugin.ajustes().debug()) {
                    plugin.getLogger().info("JPRewards esta actualizado (version " + ultimaVersion + ").");
                }
            }
        });
    }

    private String consultar(int recurso) {
        HttpURLConnection conexion = null;
        try {
            URL url = new URL(API + recurso);
            conexion = (HttpURLConnection) url.openConnection();
            conexion.setRequestMethod("GET");
            conexion.setConnectTimeout(5000);
            conexion.setReadTimeout(5000);
            conexion.setRequestProperty("User-Agent", "JPRewards/" + plugin.getDescription().getVersion());
            if (conexion.getResponseCode() != 200) {
                return null;
            }
            try (BufferedReader lector = new BufferedReader(
                    new InputStreamReader(conexion.getInputStream(), StandardCharsets.UTF_8))) {
                return lector.readLine();
            }
        } catch (Throwable error) {
            if (plugin.ajustes().debug()) {
                plugin.getLogger().info("Could not check the version: " + error.getMessage());
            }
            return null;
        } finally {
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    /** Compara dos versiones tipo "2.0.0" / "2.1". Devuelve >0 si 'nueva' es mayor. */
    public static int comparar(String nueva, String actual) {
        String[] partesNueva = limpiar(nueva).split("\\.");
        String[] partesActual = limpiar(actual).split("\\.");
        int largo = Math.max(partesNueva.length, partesActual.length);
        for (int i = 0; i < largo; i++) {
            int valorNueva = i < partesNueva.length ? numero(partesNueva[i]) : 0;
            int valorActual = i < partesActual.length ? numero(partesActual[i]) : 0;
            if (valorNueva != valorActual) {
                return valorNueva - valorActual;
            }
        }
        return 0;
    }

    private static String limpiar(String version) {
        return version == null ? "0" : version.trim().replaceAll("[^0-9.]", "");
    }

    private static int numero(String parte) {
        try {
            return Integer.parseInt(parte.trim());
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    public boolean hayNueva() {
        return hayNueva;
    }

    public String ultimaVersion() {
        return ultimaVersion;
    }
}
