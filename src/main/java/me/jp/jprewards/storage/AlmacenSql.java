package me.jp.jprewards.storage;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.DatosJugador;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Almacenamiento en base de datos por JDBC (MySQL / MariaDB).
 *
 * El plugin NO incluye ningun driver: usa el que ya trae el servidor (Spigot
 * y Paper incluyen el de MySQL), por eso el .jar sigue sin dependencias. Si
 * el servidor no lo trae se puede indicar otro en config.yml.
 *
 * Esquema (una sola tabla, muy facil de respaldar):
 *
 *   CREATE TABLE <prefijo>jugadores (
 *     uuid       VARCHAR(36) PRIMARY KEY,
 *     nombre     VARCHAR(16),
 *     datos      TEXT NOT NULL,     -- los mismos datos que el archivo YAML
 *     actualizado BIGINT NOT NULL
 *   );
 *
 * Guardar los datos como texto YAML (en vez de una columna por campo) hace
 * que agregar campos nuevos en el futuro NO necesite migrar la tabla, y que
 * se pueda mover la informacion entre YAML y la base en los dos sentidos.
 */
public final class AlmacenSql implements Almacen {

    /** Drivers que se prueban cuando 'clase-driver' esta vacio. */
    private static final String[] DRIVERS = {
            "com.mysql.cj.jdbc.Driver",
            "com.mysql.jdbc.Driver",
            "org.mariadb.jdbc.Driver"
    };

    private final JPRewards plugin;
    private final AjustesSql ajustes;
    private Connection conexion;

    public AlmacenSql(JPRewards plugin, AjustesSql ajustes) {
        this.plugin = plugin;
        this.ajustes = ajustes;
    }

    // ------------------------------------------------------------------

    @Override
    public synchronized void iniciar() throws Exception {
        cargarDriver();
        // almacenamiento.mysql.reintentos: intentos extra (con una espera
        // corta) antes de darse por vencido y caer a YAML.
        int intentos = Math.max(1, ajustes.reintentos() + 1);
        SQLException ultimo = null;
        for (int intento = 1; intento <= intentos; intento++) {
            try {
                conectar();
                crearTabla();
                if (intento > 1) {
                    plugin.getLogger().info("Database connection established on attempt " + intento + ".");
                }
                return;
            } catch (SQLException error) {
                ultimo = error;
                plugin.getLogger().warning("Attempt " + intento + "/" + intentos
                        + " of connecting to the database failed: " + error.getMessage());
                if (intento < intentos) {
                    try {
                        Thread.sleep(1500L * intento);
                    } catch (InterruptedException interrumpido) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw ultimo == null ? new SQLException("Could not connect to the database") : ultimo;
    }

    private void cargarDriver() throws SQLException {
        List<String> candidatos = new ArrayList<String>();
        if (ajustes.claseDriver() != null && !ajustes.claseDriver().trim().isEmpty()) {
            candidatos.add(ajustes.claseDriver().trim());
        } else {
            for (String driver : DRIVERS) {
                candidatos.add(driver);
            }
        }
        for (String nombre : candidatos) {
            try {
                Class<?> tipo = Class.forName(nombre);
                Object instancia = tipo.getDeclaredConstructor().newInstance();
                if (instancia instanceof Driver) {
                    plugin.getLogger().info("Using JDBC driver " + nombre + ".");
                    return;
                }
            } catch (Throwable noDisponible) {
                // se prueba el siguiente
            }
        }
        throw new SQLException("No hay ningun driver JDBC disponible (se probo: " + String.join(", ", candidatos)
                + "). Indica uno en almacenamiento.mysql.clase-driver.");
    }

    private void conectar() throws SQLException {
        String url = ajustes.url();
        conexion = DriverManager.getConnection(url, ajustes.usuario(), ajustes.contrasena());
        if (!conexion.getAutoCommit()) {
            conexion.setAutoCommit(true);
        }
    }

    private synchronized Connection conexion() throws SQLException {
        if (conexion == null || conexion.isClosed() || !conexion.isValid(3)) {
            plugin.getLogger().warning("The database connection was lost; reconnecting.");
            conectar();
        }
        return conexion;
    }

    /** true si el destino es MySQL/MariaDB (acepta MEDIUMTEXT). */
    private boolean esMysql() {
        String url = ajustes.url().toLowerCase(java.util.Locale.ROOT);
        return url.startsWith("jdbc:mysql") || url.startsWith("jdbc:mariadb");
    }

    private void crearTabla() throws SQLException {
        // MEDIUMTEXT en MySQL/MariaDB (16 MB) para que quepan hasta muchos
        // items pendientes; TEXT (64 KB) si el servidor usa otro motor.
        String tipoTexto = esMysql() ? "MEDIUMTEXT" : "TEXT";
        String sql = "CREATE TABLE IF NOT EXISTS " + ajustes.tablaJugadores() + " ("
                + "uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "nombre VARCHAR(16), "
                + "datos " + tipoTexto + " NOT NULL, "
                + "actualizado BIGINT NOT NULL)";
        try (Statement sentencia = conexion().createStatement()) {
            sentencia.executeUpdate(sql);
        }
    }

    // ------------------------------------------------------------------

    @Override
    public synchronized DatosJugador cargar(UUID uuid, String nombre) {
        String sql = "SELECT datos FROM " + ajustes.tablaJugadores() + " WHERE uuid = ?";
        try (PreparedStatement sentencia = conexion().prepareStatement(sql)) {
            sentencia.setString(1, uuid.toString());
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (resultado.next()) {
                    DatosJugador datos = DatosCodec.desdeTexto(uuid, resultado.getString("datos"));
                    if (nombre != null && !nombre.isEmpty()) {
                        datos.nombre(nombre);
                    }
                    return datos;
                }
            }
        } catch (SQLException error) {
            throw new ErrorAlmacen("Could not read the data of " + uuid + ": " + error.getMessage(), error);
        }
        DatosJugador datos = new DatosJugador(uuid);
        datos.nombre(nombre);
        return datos;
    }

    @Override
    public synchronized void guardarTexto(UUID uuid, String nombre, String texto) {
        guardarLote(java.util.Collections.singletonMap(uuid, new String[]{nombre, texto}));
    }

    @Override
    public synchronized void guardarVarios(Collection<DatosJugador> jugadores) {
        if (jugadores == null || jugadores.isEmpty()) {
            return;
        }
        java.util.Map<UUID, String[]> lote = new java.util.LinkedHashMap<UUID, String[]>();
        for (DatosJugador datos : jugadores) {
            lote.put(datos.uuid(), new String[]{datos.nombre(), DatosCodec.aTexto(datos)});
        }
        guardarLote(lote);
    }

    private void guardarLote(java.util.Map<UUID, String[]> jugadores) {
        if (jugadores == null || jugadores.isEmpty()) {
            return;
        }
        String actualizar = "UPDATE " + ajustes.tablaJugadores() + " SET nombre = ?, datos = ?, actualizado = ? WHERE uuid = ?";
        String insertar = "INSERT INTO " + ajustes.tablaJugadores() + " (uuid, nombre, datos, actualizado) VALUES (?, ?, ?, ?)";
        long ahora = System.currentTimeMillis();
        try {
            for (java.util.Map.Entry<UUID, String[]> entrada : jugadores.entrySet()) {
                UUID uuid = entrada.getKey();
                String nombre = recortarNombre(entrada.getValue()[0]);
                String texto = entrada.getValue()[1];
                boolean actualizado;
                try (PreparedStatement sentencia = conexion().prepareStatement(actualizar)) {
                    sentencia.setString(1, nombre);
                    sentencia.setString(2, texto);
                    sentencia.setLong(3, ahora);
                    sentencia.setString(4, uuid.toString());
                    actualizado = sentencia.executeUpdate() > 0;
                }
                if (!actualizado) {
                    try (PreparedStatement sentencia = conexion().prepareStatement(insertar)) {
                        sentencia.setString(1, uuid.toString());
                        sentencia.setString(2, nombre);
                        sentencia.setString(3, texto);
                        sentencia.setLong(4, ahora);
                        sentencia.executeUpdate();
                    } catch (SQLException yaExiste) {
                        // Otro hilo lo inserto entre el UPDATE y el INSERT: se reintenta.
                        try (PreparedStatement sentencia = conexion().prepareStatement(actualizar)) {
                            sentencia.setString(1, nombre);
                            sentencia.setString(2, texto);
                            sentencia.setLong(3, ahora);
                            sentencia.setString(4, uuid.toString());
                            sentencia.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException error) {
            throw new ErrorAlmacen("Could not save the data to the database: " + error.getMessage(), error);
        }
    }

    private static String recortarNombre(String nombre) {
        if (nombre == null) {
            return "";
        }
        return nombre.length() > 16 ? nombre.substring(0, 16) : nombre;
    }

    @Override
    public synchronized Set<UUID> conocidos() {
        Set<UUID> jugadores = new LinkedHashSet<UUID>();
        String sql = "SELECT uuid FROM " + ajustes.tablaJugadores();
        try (PreparedStatement sentencia = conexion().prepareStatement(sql);
                ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                try {
                    jugadores.add(UUID.fromString(resultado.getString("uuid")));
                } catch (IllegalArgumentException ignorado) {
                    // uuid invalido en la tabla: se ignora
                }
            }
        } catch (SQLException error) {
            throw new ErrorAlmacen("Could not read the player list: " + error.getMessage(), error);
        }
        return jugadores;
    }

    @Override
    public synchronized void eliminar(UUID uuid) {
        String sql = "DELETE FROM " + ajustes.tablaJugadores() + " WHERE uuid = ?";
        try (PreparedStatement sentencia = conexion().prepareStatement(sql)) {
            sentencia.setString(1, uuid.toString());
            sentencia.executeUpdate();
        } catch (SQLException error) {
            throw new ErrorAlmacen("Could not delete player " + uuid + ": " + error.getMessage(), error);
        }
    }

    @Override
    public String describir() {
        return "MySQL (" + ajustes.describir() + ", tabla '" + ajustes.tablaJugadores() + "')";
    }

    @Override
    public synchronized void cerrar() {
        if (conexion == null) {
            return;
        }
        try {
            if (!conexion.isClosed()) {
                conexion.close();
            }
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not close the database connection: " + error.getMessage());
        }
        conexion = null;
    }

    /** true si la conexion esta abierta (lo usa /rewards info). */
    public synchronized boolean conectado() {
        try {
            return conexion != null && !conexion.isClosed();
        } catch (SQLException error) {
            return false;
        }
    }
}
