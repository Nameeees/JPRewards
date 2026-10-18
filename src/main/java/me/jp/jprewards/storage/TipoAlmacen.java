package me.jp.jprewards.storage;

/**
 * Donde se guardan los datos de los jugadores.
 */
public enum TipoAlmacen {

    /** Archivos YAML dentro de la carpeta del plugin (por defecto). */
    YAML("yaml"),
    /** Base de datos por JDBC (MySQL / MariaDB). */
    MYSQL("mysql");

    private final String clave;

    TipoAlmacen(String clave) {
        this.clave = clave;
    }

    public String clave() {
        return clave;
    }

    /** Devuelve null si el texto no corresponde a ningun tipo conocido. */
    public static TipoAlmacen desde(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().toLowerCase(java.util.Locale.ROOT);
        for (TipoAlmacen tipo : values()) {
            if (tipo.clave.equals(limpio)) {
                return tipo;
            }
        }
        return null;
    }
}
