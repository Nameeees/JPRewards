package me.jp.jprewards.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Tiempo del plugin: clave de dia (con hora de corte configurable), mes,
 * semana, cuentas atras y formateo de fechas.
 *
 * Se usa Calendar (Java 7+) en vez de java.time para que el plugin funcione
 * tambien en servidores 1.8 con Java 8 antiguo.
 *
 * Idea clave: TODO se mide con "claves" (numeros) en vez de fechas. Dos
 * instantes con la misma clave pertenecen al mismo dia/semana/mes del juego,
 * asi que comparar claves es comparar periodos y no hay errores de zona
 * horaria ni de cambios de hora.
 */
public final class Tiempo {

    private static final long DIA_MS = 86400000L;
    private static final String[] DIAS = {
            "domingo", "lunes", "martes", "miercoles", "jueves", "viernes", "sabado"
    };

    private Tiempo() {
    }

    /** Milisegundos actuales (atajo, para tests y logs). */
    public static long ahora() {
        return System.currentTimeMillis();
    }

    // ------------------------------------------------------------------
    // Dias
    // ------------------------------------------------------------------

    /** Numero de dia absoluto teniendo en cuenta la zona horaria y la hora de corte. */
    public static long claveDia(int horaCorte) {
        return claveDiaDe(System.currentTimeMillis(), horaCorte);
    }

    /** Igual que claveDia pero para un instante concreto (util en tests). */
    public static long claveDiaDe(long milisegundos, int horaCorte) {
        long desplazado = milisegundos + TimeZone.getDefault().getOffset(milisegundos) - (horaCorte * 3600000L);
        return Math.floorDiv(desplazado, DIA_MS);
    }

    /** Milisegundos que faltan para el proximo corte diario. */
    public static long msHastaProximoCorte(int horaCorte) {
        long milisegundos = System.currentTimeMillis();
        long desplazado = milisegundos + TimeZone.getDefault().getOffset(milisegundos) - (horaCorte * 3600000L);
        return DIA_MS - Math.floorMod(desplazado, DIA_MS);
    }

    /**
     * Cuenta atras hasta el proximo corte diario, en formato legible:
     * "03h 24m" si falta mas de una hora, "24m 51s" si falta menos.
     */
    public static String restante(int horaCorte) {
        long segundos = msHastaProximoCorte(horaCorte) / 1000L;
        long horas = segundos / 3600L;
        long minutos = (segundos % 3600L) / 60L;
        long segs = segundos % 60L;
        if (horas > 0) {
            return String.format("%02dh %02dm", horas, minutos);
        }
        return String.format("%02dm %02ds", minutos, segs);
    }

    /** Cuenta atras larga ("1d 03h 24m"), util para reinicios lejanos. */
    public static String restanteLargo(int horaCorte) {
        return duracion(msHastaProximoCorte(horaCorte));
    }

    /** Dias transcurridos entre dos claves de dia. */
    public static int diasEntre(long claveA, long claveB) {
        return (int) (claveB - claveA);
    }

    // ------------------------------------------------------------------
    // Meses
    // ------------------------------------------------------------------

    /** Clave del mes actual: anyo * 100 + mes (p. ej. 202603). */
    public static int claveMes(int horaCorte) {
        Calendar calendario = calendarioActual(horaCorte);
        return calendario.get(Calendar.YEAR) * 100 + (calendario.get(Calendar.MONTH) + 1);
    }

    /** Numero de dias del mes actual (28-31). */
    public static int diasDelMesActual(int horaCorte) {
        return calendarioActual(horaCorte).getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /** Numero del mes de una clave (1-12). */
    public static int mesDeClave(int claveMes) {
        return claveMes % 100;
    }

    // ------------------------------------------------------------------
    // Semanas
    // ------------------------------------------------------------------

    /**
     * Dia de inicio de semana a partir de un nombre en espanol o ingles.
     * Devuelve una constante de Calendar (Calendar.MONDAY por defecto).
     */
    public static int diaSemana(String nombre) {
        if (nombre == null) {
            return Calendar.MONDAY;
        }
        String limpio = normalizar(nombre);
        if ("domingo".equals(limpio) || "sunday".equals(limpio)) {
            return Calendar.SUNDAY;
        }
        if ("martes".equals(limpio) || "tuesday".equals(limpio)) {
            return Calendar.TUESDAY;
        }
        if ("miercoles".equals(limpio) || "wednesday".equals(limpio)) {
            return Calendar.WEDNESDAY;
        }
        if ("jueves".equals(limpio) || "thursday".equals(limpio)) {
            return Calendar.THURSDAY;
        }
        if ("viernes".equals(limpio) || "friday".equals(limpio)) {
            return Calendar.FRIDAY;
        }
        if ("sabado".equals(limpio) || "saturday".equals(limpio)) {
            return Calendar.SATURDAY;
        }
        return Calendar.MONDAY;
    }

    /** Nombre en espanol de una constante de Calendar (para /info). */
    public static String nombreDia(int constante) {
        int indice = constante - 1;
        if (indice < 0 || indice >= DIAS.length) {
            return "lunes";
        }
        return DIAS[indice];
    }

    /** true si el nombre del dia es valido. */
    public static boolean diaValido(String nombre) {
        if (nombre == null) {
            return false;
        }
        String limpio = normalizar(nombre);
        for (String dia : DIAS) {
            if (dia.equals(limpio)) {
                return true;
            }
        }
        return "sunday".equals(limpio) || "monday".equals(limpio) || "tuesday".equals(limpio)
                || "wednesday".equals(limpio) || "thursday".equals(limpio) || "friday".equals(limpio)
                || "saturday".equals(limpio);
    }

    /** Clave de semana alineada al dia de inicio configurado. */
    public static long claveSemana(int horaCorte, int diaInicio) {
        long milisegundos = System.currentTimeMillis();
        long desplazado = milisegundos + TimeZone.getDefault().getOffset(milisegundos) - (horaCorte * 3600000L);
        long diaAbsoluto = Math.floorDiv(desplazado, DIA_MS);
        return Math.floorDiv(diaAbsoluto - distanciaInicioSemana(horaCorte, diaInicio), 7);
    }

    /** Cuenta atras hasta el proximo inicio de semana: "2d 03h 12m". */
    public static String restanteSemana(int horaCorte, int diaInicio) {
        long milisegundos = System.currentTimeMillis();
        long desplazado = milisegundos + TimeZone.getDefault().getOffset(milisegundos) - (horaCorte * 3600000L);
        long enDia = Math.floorMod(desplazado, DIA_MS);
        long distancia = distanciaInicioSemana(horaCorte, diaInicio);
        long faltaMs = ((7L - distancia) * DIA_MS) - enDia;
        return duracion(Math.max(0L, faltaMs));
    }

    /** Dias transcurridos desde el inicio de la semana actual (0..6). */
    private static int distanciaInicioSemana(int horaCorte, int diaInicio) {
        Calendar calendario = calendarioActual(horaCorte);
        int diaDeLaSemana = calendario.get(Calendar.DAY_OF_WEEK);
        return (diaDeLaSemana - diaInicio + 7) % 7;
    }

    // ------------------------------------------------------------------
    // Formato
    // ------------------------------------------------------------------

    /** Fecha legible (dd/MM/yyyy) a partir de una clave de dia. */
    public static String fechaDesdeClave(long clave) {
        if (clave <= 0) {
            return "-";
        }
        return new SimpleDateFormat("dd/MM/yyyy").format(new Date(clave * DIA_MS));
    }

    /** Fecha y hora legibles a partir de milisegundos. */
    public static String fechaHora(long milisegundos) {
        if (milisegundos <= 0) {
            return "-";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(milisegundos));
    }

    /**
     * Duracion legible: "3d 04h 12m", "04h 12m", "12m 30s" o "30s".
     * Se usa para reinicios, avisos y /info.
     */
    public static String duracion(long milisegundos) {
        long segundos = Math.max(0L, milisegundos / 1000L);
        long dias = segundos / 86400L;
        long horas = (segundos % 86400L) / 3600L;
        long minutos = (segundos % 3600L) / 60L;
        long segs = segundos % 60L;
        if (dias > 0) {
            return String.format("%dd %02dh %02dm", dias, horas, minutos);
        }
        if (horas > 0) {
            return String.format("%02dh %02dm", horas, minutos);
        }
        if (minutos > 0) {
            return String.format("%02dm %02ds", minutos, segs);
        }
        return String.format("%ds", segs);
    }

    /** Hora del dia (HH:mm) del proximo corte. */
    public static String horaProximoCorte(int horaCorte) {
        return new SimpleDateFormat("HH:mm").format(new Date(System.currentTimeMillis() + msHastaProximoCorte(horaCorte)));
    }

    private static String normalizar(String texto) {
        return texto.trim().toLowerCase(Locale.ROOT)
                .replace('\u00e1', 'a')
                .replace('\u00e9', 'e')
                .replace('\u00ed', 'i')
                .replace('\u00f3', 'o')
                .replace('\u00fa', 'u')
                .replace('\u00fc', 'u');
    }

    private static Calendar calendarioActual(int horaCorte) {
        Calendar calendario = Calendar.getInstance();
        calendario.setTimeInMillis(System.currentTimeMillis() - (horaCorte * 3600000L));
        return calendario;
    }
}
