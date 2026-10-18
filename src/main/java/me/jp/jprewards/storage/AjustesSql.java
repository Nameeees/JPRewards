package me.jp.jprewards.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Ajustes de la base de datos (almacenamiento.mysql.* de config.yml).
 *
 * El plugin NO incluye ningun driver JDBC: usa el que ya trae el servidor
 * (Spigot y Paper incluyen el de MySQL/MariaDB), asi el .jar no lleva
 * dependencias. Si hace falta otro motor se puede indicar 'clase-driver'.
 */
public final class AjustesSql {

    private String host = "127.0.0.1";
    private int puerto = 3306;
    private String baseDeDatos = "jprewards";
    private String usuario = "root";
    private String contrasena = "";
    private String prefijoTablas = "jprewards_";
    private String propiedades = "useSSL=false&characterEncoding=utf8";
    private String urlJdbc = "";
    private String claseDriver = "";
    private int reintentos = 2;

    public static AjustesSql leer(FileConfiguration config) {
        AjustesSql ajustes = new AjustesSql();
        ConfigurationSection seccion = config.getConfigurationSection("almacenamiento.mysql");
        if (seccion == null) {
            return ajustes;
        }
        ajustes.host = seccion.getString("host", ajustes.host);
        ajustes.puerto = Math.max(1, Math.min(65535, seccion.getInt("puerto", ajustes.puerto)));
        ajustes.baseDeDatos = seccion.getString("base-de-datos", ajustes.baseDeDatos);
        ajustes.usuario = seccion.getString("usuario", ajustes.usuario);
        ajustes.contrasena = seccion.getString("contrasena", ajustes.contrasena);
        ajustes.prefijoTablas = limpiarPrefijo(seccion.getString("prefijo-tablas", ajustes.prefijoTablas));
        ajustes.propiedades = seccion.getString("propiedades", ajustes.propiedades);
        ajustes.urlJdbc = seccion.getString("url-jdbc", "");
        ajustes.claseDriver = seccion.getString("clase-driver", "");
        ajustes.reintentos = Math.max(0, Math.min(10, seccion.getInt("reintentos", 2)));
        return ajustes;
    }

    /** Solo letras, numeros y guion bajo: evita inyeccion en el nombre de tabla. */
    private static String limpiarPrefijo(String valor) {
        if (valor == null) {
            return "jprewards_";
        }
        String limpio = valor.replaceAll("[^A-Za-z0-9_]", "");
        return limpio.isEmpty() ? "jprewards_" : limpio;
    }

    /** URL JDBC final: la configurada, o la construida con host/puerto/base. */
    public String url() {
        if (urlJdbc != null && !urlJdbc.trim().isEmpty()) {
            return urlJdbc.trim();
        }
        String extras = propiedades == null || propiedades.trim().isEmpty() ? "" : "?" + propiedades.trim();
        return "jdbc:mysql://" + host + ":" + puerto + "/" + baseDeDatos + extras;
    }

    public String host() {
        return host;
    }

    public int puerto() {
        return puerto;
    }

    public String baseDeDatos() {
        return baseDeDatos;
    }

    public String usuario() {
        return usuario;
    }

    public String contrasena() {
        return contrasena;
    }

    public String tablaJugadores() {
        return prefijoTablas + "jugadores";
    }

    public String prefijoTablas() {
        return prefijoTablas;
    }

    public String propiedades() {
        return propiedades;
    }

    public String urlJdbc() {
        return urlJdbc;
    }

    public String claseDriver() {
        return claseDriver;
    }

    public int reintentos() {
        return reintentos;
    }

    /** Texto para /rewards info: destino sin contrasena. */
    public String describir() {
        if (urlJdbc != null && !urlJdbc.trim().isEmpty()) {
            return urlJdbc.trim();
        }
        return host + ":" + puerto + "/" + baseDeDatos;
    }
}
