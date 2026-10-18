package me.jp.testing;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Recoge el resultado de las comprobaciones y lo imprime.
 *
 * Formato de salida (es el que lee testing/run-tests.ps1):
 *   CHECK OK: <texto>
 *   CHECK FAILED: <texto>
 *   CHECK SKIPPED: <texto>
 *   HARNESS RESULT checks=<n> failures=<n> skipped=<n>
 *   HARNESS PASS | HARNESS FAIL
 *
 * Reglas:
 *   - Una comprobacion que no se puede hacer porque la clase o el metodo
 *     todavia no existen se marca SKIPPED (no rompe el arranque).
 *   - Una comprobacion que existe y falla se marca FAILED.
 *   - Ninguna excepcion sale de aqui: el resumen y el apagado se hacen igual.
 */
public final class Registro {

    /** Trabajo de una comprobacion (puede lanzar lo que sea). */
    public interface Comprobacion {
        void ejecutar() throws Throwable;
    }

    private final PrintStream salida;
    private int total;
    private int fallos;
    private int saltados;
    private final List<String> detalleFallos = new ArrayList<String>();
    private final List<String> detalleSaltados = new ArrayList<String>();

    public Registro(PrintStream salida) {
        this.salida = salida;
    }

    public int total() {
        return total;
    }

    public int fallos() {
        return fallos;
    }

    public int saltados() {
        return saltados;
    }

    /** Linea informativa del harness (no cuenta como comprobacion). */
    public void info(String texto) {
        salida.println("HARNESS INFO: " + texto);
        salida.flush();
    }

    public void ok(String texto) {
        total++;
        salida.println("CHECK OK: " + texto);
        salida.flush();
    }

    public void fallo(String texto) {
        total++;
        fallos++;
        detalleFallos.add(texto);
        salida.println("CHECK FAILED: " + texto);
        salida.flush();
    }

    public void saltado(String texto) {
        total++;
        saltados++;
        detalleSaltados.add(texto);
        salida.println("CHECK SKIPPED: " + texto);
        salida.flush();
    }

    /** Decide entre OK y FAILED segun el booleano. */
    public void comprobar(boolean correcto, String textoOk, String textoFallo) {
        if (correcto) {
            ok(textoOk);
        } else {
            fallo(textoFallo);
        }
    }

    /** Ejecuta una comprobacion: si lanza, cuenta como fallo y se sigue. */
    public void seguro(String texto, Comprobacion comprobacion) {
        try {
            comprobacion.ejecutar();
        } catch (Throwable error) {
            fallo(texto + " -> excepcion: " + describir(error));
        }
    }

    /** Texto corto de un error (clase + mensaje). */
    public static String describir(Throwable error) {
        if (error == null) {
            return "error desconocido";
        }
        String mensaje = error.getMessage();
        return error.getClass().getName() + (mensaje == null ? "" : ": " + mensaje);
    }

    /** Imprime el resumen final y si el conjunto pasa o no. */
    public void imprimirResumen() {
        if (!detalleFallos.isEmpty()) {
            salida.println("HARNESS FALLOS:");
            for (String detalle : detalleFallos) {
                salida.println("  - " + detalle);
            }
        }
        if (!detalleSaltados.isEmpty()) {
            salida.println("HARNESS SALTADOS:");
            for (String detalle : detalleSaltados) {
                salida.println("  - " + detalle);
            }
        }
        salida.println("HARNESS RESULT checks=" + total + " failures=" + fallos + " skipped=" + saltados);
        salida.println(fallos == 0 ? "HARNESS PASS" : "HARNESS FAIL");
        salida.flush();
    }
}
