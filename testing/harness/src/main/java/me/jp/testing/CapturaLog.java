package me.jp.testing;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Captura lo que se registra en el log del servidor mientras esta armada.
 *
 * Sirve para comprobar que un comando no ha lanzado una excepcion aunque el
 * servidor se la haya comido: si el servidor atrapa la excepcion y la escribe
 * en el log en vez de relanzarla, aqui queda registrada.
 *
 * Se engancha al logger "Minecraft" (el que usa Bukkit para todo) y al logger
 * raiz, y descarta los registros repetidos (un mismo registro puede pasar por
 * los dos). Los mensajes propios del harness (marcados con [harness]) se
 * ignoran.
 */
public final class CapturaLog extends Handler {

    /** Palabras que delatan un error en el log. */
    private static final String[] SENALES = {
            "exception", "unhandled", "could not pass event", "caused by", "stacktrace",
            "noclassdeffound", "nosuchmethod", "cannot invoke", "nullpointer"
    };

    private final List<String> sucesos = new ArrayList<String>();
    private final Set<String> vistos = new LinkedHashSet<String>();
    private volatile boolean armado;

    /** Engancha el capturador a los loggers del servidor. */
    public void instalar() {
        setLevel(Level.ALL);
        try {
            Logger.getLogger("Minecraft").addHandler(this);
        } catch (Throwable ignorado) {
            // Sin logger "Minecraft" se sigue con el raiz.
        }
        try {
            Logger.getLogger("").addHandler(this);
        } catch (Throwable ignorado) {
            // Sin logger raiz no se captura nada mas: no es critico.
        }
    }

    public void quitar() {
        try {
            Logger.getLogger("Minecraft").removeHandler(this);
        } catch (Throwable ignorado) {
        }
        try {
            Logger.getLogger("").removeHandler(this);
        } catch (Throwable ignorado) {
        }
    }

    public void armar() {
        armado = true;
    }

    public void desarmar() {
        armado = false;
    }

    public void limpiar() {
        sucesos.clear();
        vistos.clear();
    }

    /** Sucesos con pinta de error capturados desde el ultimo limpiar(). */
    public List<String> sucesos() {
        return new ArrayList<String>(sucesos);
    }

    @Override
    public void publish(LogRecord registro) {
        if (!armado || registro == null) {
            return;
        }
        String mensaje = registro.getMessage();
        Throwable lanzado = registro.getThrown();
        if (mensaje != null && mensaje.contains("[harness]")) {
            return;
        }
        boolean sospechoso = lanzado != null || tieneSenal(mensaje);
        if (!sospechoso) {
            return;
        }
        String texto = describir(registro, lanzado);
        String huella = texto;
        if (vistos.add(huella)) {
            sucesos.add(texto);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    private static boolean tieneSenal(String mensaje) {
        if (mensaje == null) {
            return false;
        }
        String plano = mensaje.toLowerCase(java.util.Locale.ROOT);
        for (String senal : SENALES) {
            if (plano.contains(senal)) {
                return true;
            }
        }
        return false;
    }

    private static String describir(LogRecord registro, Throwable lanzado) {
        StringBuilder texto = new StringBuilder();
        texto.append("[").append(registro.getLevel()).append("] ");
        if (registro.getMessage() != null) {
            texto.append(registro.getMessage());
        }
        if (lanzado != null) {
            texto.append(" -> ").append(lanzado.getClass().getName());
            if (lanzado.getMessage() != null) {
                texto.append(": ").append(lanzado.getMessage());
            }
            StackTraceElement[] pila = lanzado.getStackTrace();
            if (pila != null && pila.length > 0) {
                texto.append(" (en ").append(pila[0]).append(")");
            }
        }
        return texto.toString();
    }
}
