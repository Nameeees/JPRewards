package me.jp.jprewards.util;

import org.bukkit.ChatColor;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Textos del plugin: color, color hexadecimal, pseudo-gradiente, numeros,
 * placeholders y barras de progreso.
 *
 * Todo el plugin usa codigos legacy con & (compatibles de 1.8 a la ultima
 * version). El formato &#RRGGBB se soporta en 1.16+ por reflexion y en
 * versiones antiguas se elimina el codigo (queda el texto, sin romper nada).
 */
public final class Texto {

    private static final DecimalFormat NUMERO;
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern CODIGO = Pattern.compile("[\u00A7&][0-9a-fk-orA-FK-OR]");

    private static Method colorDe;
    private static boolean colorDeBuscado;

    static {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.US);
        NUMERO = new DecimalFormat("#,##0.##", simbolos);
    }

    private Texto() {
    }

    // ------------------------------------------------------------------
    // Color
    // ------------------------------------------------------------------

    /** Traduce codigos & (y &#RRGGBB en 1.16+) a colores reales. */
    public static String color(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', hex(texto));
    }

    /**
     * Sustituye &#RRGGBB por el color real si el servidor lo soporta (1.16+).
     * En versiones antiguas se quita el codigo y queda solo el texto, asi que
     * un mismo archivo de configuracion vale para todas las versiones.
     */
    private static String hex(String texto) {
        if (texto.indexOf("&#") < 0) {
            return texto;
        }
        boolean soporta = soportaHex();
        Matcher buscador = HEX.matcher(texto);
        StringBuffer salida = new StringBuffer();
        while (buscador.find()) {
            String reemplazo = "";
            if (soporta) {
                try {
                    Object color = colorDe.invoke(null, "#" + buscador.group(1));
                    reemplazo = color == null ? "" : color.toString();
                } catch (Throwable ignorado) {
                    reemplazo = "";
                }
            }
            buscador.appendReplacement(salida, Matcher.quoteReplacement(reemplazo));
        }
        buscador.appendTail(salida);
        return salida.toString();
    }

    /** ChatColor#of existe desde 1.16. */
    private static boolean soportaHex() {
        if (!colorDeBuscado) {
            colorDeBuscado = true;
            try {
                colorDe = ChatColor.class.getMethod("of", String.class);
            } catch (Throwable sinHex) {
                colorDe = null;
            }
        }
        return colorDe != null;
    }

    /** Quita todos los codigos de color (para medir o para logs). */
    public static String sinColor(String texto) {
        if (texto == null) {
            return "";
        }
        return ChatColor.stripColor(color(texto));
    }

    // ------------------------------------------------------------------
    // Gradiente y adornos
    // ------------------------------------------------------------------

    /**
     * Pseudo-gradiente legacy: alterna los colores indicados caracter a
     * caracter para simular un degradado tambien en clientes 1.8.
     * Los espacios no consumen color.
     */
    public static String gradiente(String texto, List<String> colores) {
        if (texto == null || texto.isEmpty() || colores == null || colores.isEmpty()) {
            return texto == null ? "" : texto;
        }
        StringBuilder salida = new StringBuilder();
        int indice = 0;
        for (int i = 0; i < texto.length(); i++) {
            char caracter = texto.charAt(i);
            if (caracter == '&' && i + 1 < texto.length()) {
                salida.append(caracter).append(texto.charAt(i + 1));
                i++;
                continue;
            }
            if (caracter == ' ') {
                salida.append(caracter);
                continue;
            }
            salida.append(colores.get(indice % colores.size())).append(caracter);
            indice++;
        }
        return salida.toString();
    }

    /**
     * Barra de progreso de texto: barra(3, 10, 10, "|", "|", "&a", "&7").
     * Se usa en el placeholder {barra_mes} de los menus.
     */
    public static String barra(int actual, int total, int largo, String lleno, String vacio, String colorLleno, String colorVacio) {
        int tamano = Math.max(1, largo);
        int valor = total <= 0 ? 0 : Math.max(0, Math.min(tamano, (int) Math.round((double) actual * tamano / total)));
        StringBuilder salida = new StringBuilder();
        salida.append(colorLleno);
        for (int i = 0; i < valor; i++) {
            salida.append(lleno);
        }
        salida.append(colorVacio);
        for (int i = valor; i < tamano; i++) {
            salida.append(vacio);
        }
        return salida.toString();
    }

    // ------------------------------------------------------------------
    // Numeros y medidas
    // ------------------------------------------------------------------

    /** Numero formateado para mensajes: 1250.5 -> "1,250.5". */
    public static String numero(double valor) {
        return NUMERO.format(valor);
    }

    /** Numero formateado y coloreado segun el signo (+1,250 / -320). */
    public static String numeroConSigno(double valor) {
        return (valor > 0 ? "+" : "") + numero(valor);
    }

    /**
     * Recorta un texto al maximo de caracteres VISIBLES (los titulos de
     * inventario admiten 32) conservando los codigos de color.
     */
    public static String limitar(String texto, int maximoVisible) {
        if (texto == null || maximoVisible <= 0) {
            return "";
        }
        if (visibles(texto) <= maximoVisible) {
            return texto;
        }
        StringBuilder salida = new StringBuilder();
        int contados = 0;
        for (int i = 0; i < texto.length(); i++) {
            char caracter = texto.charAt(i);
            if ((caracter == '\u00A7' || caracter == '&') && i + 1 < texto.length()) {
                // Los codigos de color se copian siempre: no ocupan espacio.
                salida.append(caracter).append(texto.charAt(i + 1));
                i++;
                continue;
            }
            if (contados >= maximoVisible) {
                break;
            }
            salida.append(caracter);
            contados++;
        }
        return salida.toString();
    }

    // ------------------------------------------------------------------
    // Placeholders
    // ------------------------------------------------------------------

    /** Reemplaza {claves} de un mapa sobre un texto. */
    public static String reemplazar(String texto, Map<String, String> placeholders) {
        if (texto == null) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return texto;
        }
        String resultado = texto;
        for (Map.Entry<String, String> entrada : placeholders.entrySet()) {
            String clave = entrada.getKey();
            if (clave == null || clave.isEmpty()) {
                continue;
            }
            String valor = entrada.getValue() == null ? "" : entrada.getValue();
            resultado = resultado.replace("{" + clave + "}", valor);
        }
        return resultado;
    }

    /** Igual que reemplazar pero sobre una lista (devuelve copia). */
    public static List<String> reemplazar(List<String> lineas, Map<String, String> placeholders) {
        List<String> resultado = new ArrayList<String>();
        if (lineas == null) {
            return resultado;
        }
        for (String linea : lineas) {
            resultado.add(reemplazar(linea, placeholders));
        }
        return resultado;
    }

    /** Texto sin codigos de color, en una sola linea (para logs). */
    public static String plano(String texto) {
        return sinColor(texto).replace('\n', ' ').trim();
    }

    /** Lista de lineas separadas por \n (util para textos de una sola clave). */
    public static List<String> lineas(String texto) {
        List<String> lista = new ArrayList<String>();
        if (texto == null || texto.isEmpty()) {
            return lista;
        }
        for (String linea : texto.split("\\n")) {
            lista.add(linea);
        }
        return lista;
    }

    /** Cuenta los caracteres visibles de un texto con colores. */
    public static int visibles(String texto) {
        return sinColor(texto).length();
    }

    /** true si el texto no tiene nada visible (vacio o solo colores). */
    public static boolean vacioVisible(String texto) {
        return CODIGO.matcher(texto == null ? "" : color(texto)).replaceAll("").trim().isEmpty();
    }
}
