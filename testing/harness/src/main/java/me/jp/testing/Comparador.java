package me.jp.testing;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compara dos DatosJugador campo por campo usando sus metodos de lectura.
 *
 * No se comparan los metodos de accion (cargar*, reiniciar, tomarPendientes...)
 * ni "sucio": lo que importa es que los DATOS sobrevivan a la ida y vuelta.
 */
public final class Comparador {

    /** Campos que el codec debe conservar. */
    public static final String[] CAMPOS = {
            "uuid", "nombre",
            "ultimoDia", "racha", "mejorRacha", "total",
            "ultimaSemana", "rachaSemanal", "mejorRachaSemanal", "totalSemanal",
            "mesClave", "diasMes", "hitosReclamados", "hitosHistoricos",
            "bonusReclamados", "rachaBonusClave",
            "primerReclamo", "ultimoReclamo", "pendientes"
    };

    private Comparador() {
    }

    /** Valor de cada campo leido con su metodo del mismo nombre. */
    public static Map<String, Object> extraer(Class<?> clase, Object datos) {
        Map<String, Object> valores = new LinkedHashMap<String, Object>();
        if (datos == null) {
            return valores;
        }
        for (String campo : CAMPOS) {
            Method metodo = Reflexion.metodo(clase, campo);
            if (metodo == null) {
                continue;
            }
            try {
                valores.put(campo, metodo.invoke(datos));
            } catch (Throwable error) {
                valores.put(campo, "<error: " + Registro.describir(error) + ">");
            }
        }
        return valores;
    }

    /** Lista de campos distintos (vacia si todo coincide). */
    public static List<String> diferencias(Class<?> clase, Object esperado, Object obtenido) {
        List<String> diferencias = new ArrayList<String>();
        if (obtenido == null) {
            diferencias.add("el resultado es null");
            return diferencias;
        }
        Map<String, Object> valoresEsperados = extraer(clase, esperado);
        Map<String, Object> valoresObtenidos = extraer(clase, obtenido);
        if (valoresEsperados.isEmpty()) {
            diferencias.add("no se pudo leer ningun campo de " + clase.getName());
            return diferencias;
        }
        for (Map.Entry<String, Object> entrada : valoresEsperados.entrySet()) {
            String campo = entrada.getKey();
            Object antes = entrada.getValue();
            Object despues = valoresObtenidos.get(campo);
            if (!iguales(antes, despues)) {
                diferencias.add(campo + " [" + texto(antes) + "] != [" + texto(despues) + "]");
            }
        }
        return diferencias;
    }

    public static boolean iguales(Object uno, Object otro) {
        if (uno == null) {
            return otro == null;
        }
        return uno.equals(otro);
    }

    /** Texto corto de un valor, para los mensajes de fallo. */
    public static String texto(Object valor) {
        if (valor == null) {
            return "null";
        }
        String texto = String.valueOf(valor);
        if (texto.length() > 140) {
            texto = texto.substring(0, 137) + "...";
        }
        return texto;
    }
}
