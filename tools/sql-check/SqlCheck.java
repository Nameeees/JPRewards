import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Comprueba las sentencias SQL de JPRewards (storage/AlmacenSql.java) contra
 * una base de datos en memoria en modo compatibilidad MySQL (H2).
 *
 * No sustituye a una prueba contra MySQL real, pero verifica que la tabla, el
 * upsert (UPDATE y, si no habia fila, INSERT) y el resto de sentencias son SQL
 * valido y hacen lo que la clase espera.
 *
 * Uso:  java -cp h2.jar tools/sql-check/SqlCheck.java   (Java 11+)
 */
public final class SqlCheck {

    private static int fallos = 0;
    private static int pruebas = 0;

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection conexion = DriverManager.getConnection(
                "jdbc:h2:mem:jprewards;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")) {
            probar(conexion, "MEDIUMTEXT");
            probar(conexion, "TEXT");
        }
        System.out.println("SQL RESULT pruebas=" + pruebas + " fallos=" + fallos);
        System.out.println(fallos == 0 ? "SQL PASS" : "SQL FAIL");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    private static void probar(Connection conexion, String tipoTexto) throws SQLException {
        String tabla = "jprewards_jugadores_" + tipoTexto.toLowerCase();
        String ddl = "CREATE TABLE IF NOT EXISTS " + tabla + " ("
                + "uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "nombre VARCHAR(16), "
                + "datos " + tipoTexto + " NOT NULL, "
                + "actualizado BIGINT NOT NULL)";
        try (Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate(ddl);
            // Repetir el CREATE no debe fallar (arranques posteriores).
            sentencia.executeUpdate(ddl);
        }
        comprobar(tipoTexto + ": CREATE TABLE IF NOT EXISTS es valido y repetible", true);

        String actualizar = "UPDATE " + tabla + " SET nombre = ?, datos = ?, actualizado = ? WHERE uuid = ?";
        String insertar = "INSERT INTO " + tabla + " (uuid, nombre, datos, actualizado) VALUES (?, ?, ?, ?)";

        int creados = 0;
        for (int i = 1; i <= 3; i++) {
            String uuid = "00000000-0000-0000-0000-00000000000" + i;
            if (upsert(conexion, actualizar, insertar, uuid, "Jugador" + i, "racha: " + i, 1000L + i)) {
                creados++;
            }
        }
        comprobar(tipoTexto + ": 3 altas con UPDATE+INSERT", creados == 3);

        // Segundo guardado del mismo jugador: tiene que actualizar, no duplicar.
        upsert(conexion, actualizar, insertar, "00000000-0000-0000-0000-000000000002", "Jugador2", "racha: 9", 2000L);
        comprobar(tipoTexto + ": el segundo guardado actualiza la fila", contar(conexion, tabla) == 3);
        comprobar(tipoTexto + ": el dato guardado se lee igual",
                "racha: 9".equals(leer(conexion, tabla, "00000000-0000-0000-0000-000000000002")));

        // Texto largo (items pendientes serializados).
        StringBuilder largo = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            largo.append("==: org.bukkit.inventory.ItemStack\n");
        }
        upsert(conexion, actualizar, insertar, "00000000-0000-0000-0000-000000000004", "Largo", largo.toString(), 3000L);
        comprobar(tipoTexto + ": admite un texto largo (" + largo.length() + " chars)",
                largo.length() == leer(conexion, tabla, "00000000-0000-0000-0000-000000000004").length());

        // Borrado (lo usa /rewards reset y el migrador).
        try (PreparedStatement sentencia = conexion.prepareStatement("DELETE FROM " + tabla + " WHERE uuid = ?")) {
            sentencia.setString(1, "00000000-0000-0000-0000-000000000001");
            sentencia.executeUpdate();
        }
        comprobar(tipoTexto + ": DELETE deja 3 filas", contar(conexion, tabla) == 3);

        // Listado de uuid (lo usa /rewards top y el migrador).
        int listados = 0;
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT uuid FROM " + tabla);
                ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) {
                if (resultado.getString("uuid") != null) {
                    listados++;
                }
            }
        }
        comprobar(tipoTexto + ": SELECT uuid devuelve las filas", listados == 3);

        // Nombres de mas de 16 caracteres se recortan en la clase; aqui se
        // comprueba que la columna VARCHAR(16) lo exige.
        boolean rechazado = false;
        try (PreparedStatement sentencia = conexion.prepareStatement(insertar)) {
            sentencia.setString(1, "00000000-0000-0000-0000-000000000009");
            sentencia.setString(2, "UnNombreDemasiadoLargo");
            sentencia.setString(3, "x");
            sentencia.setLong(4, 1L);
            sentencia.executeUpdate();
        } catch (SQLException esperado) {
            rechazado = true;
        }
        comprobar(tipoTexto + ": VARCHAR(16) rechaza nombres mas largos (por eso la clase los recorta)", rechazado);

        try (Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("DROP TABLE " + tabla);
        }
    }

    private static boolean upsert(Connection conexion, String actualizar, String insertar, String uuid, String nombre,
            String datos, long ahora) throws SQLException {
        boolean actualizado;
        try (PreparedStatement sentencia = conexion.prepareStatement(actualizar)) {
            sentencia.setString(1, nombre);
            sentencia.setString(2, datos);
            sentencia.setLong(3, ahora);
            sentencia.setString(4, uuid);
            actualizado = sentencia.executeUpdate() > 0;
        }
        if (actualizado) {
            return false;
        }
        try (PreparedStatement sentencia = conexion.prepareStatement(insertar)) {
            sentencia.setString(1, uuid);
            sentencia.setString(2, nombre);
            sentencia.setString(3, datos);
            sentencia.setLong(4, ahora);
            sentencia.executeUpdate();
            return true;
        }
    }

    private static long contar(Connection conexion, String tabla) throws SQLException {
        try (Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            resultado.next();
            return resultado.getLong(1);
        }
    }

    private static String leer(Connection conexion, String tabla, String uuid) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT datos FROM " + tabla + " WHERE uuid = ?")) {
            sentencia.setString(1, uuid);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? resultado.getString("datos") : null;
            }
        }
    }

    private static void comprobar(String texto, boolean condicion) {
        pruebas++;
        if (condicion) {
            System.out.println("CHECK OK: " + texto);
        } else {
            fallos++;
            System.out.println("CHECK FAILED: " + texto);
        }
    }
}
