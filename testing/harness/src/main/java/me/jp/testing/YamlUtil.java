package me.jp.testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidades YAML para las comprobaciones.
 *
 * Lo importante es que el archivo se carga DE VERDAD con
 * YamlConfiguration.loadConfiguration (esa es la comprobacion principal: en los
 * servidores modernos un YAML con claves repetidas puede lanzar error al
 * cargarlo). Ademas se revisan las claves repetidas a mano para que el mismo
 * problema se vea tambien en 1.8, donde el cargador es mas permisivo.
 *
 * La revision a mano entiende de listas: dos "cantidad:" en dos elementos
 * distintos de una lista NO son claves duplicadas (pertenecen a mapas
 * diferentes), asi que cada elemento de lista abre un ambito nuevo.
 */
public final class YamlUtil {

    /** Una linea "clave:" con su sangria. */
    private static final Pattern CLAVE = Pattern.compile("^( *)([^#\\s][^:]*):(.*)$");

    private YamlUtil() {
    }

    /** Texto completo del archivo en UTF-8 (vacio si no se puede leer). */
    public static String leerTexto(File archivo) {
        if (archivo == null || !archivo.isFile()) {
            return "";
        }
        StringBuilder texto = new StringBuilder();
        Reader lector = null;
        try {
            lector = new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8);
            char[] bufer = new char[4096];
            int leidos;
            while ((leidos = lector.read(bufer)) > 0) {
                texto.append(bufer, 0, leidos);
            }
        } catch (Throwable error) {
            return texto.toString();
        } finally {
            if (lector != null) {
                try {
                    lector.close();
                } catch (Throwable ignorado) {
                }
            }
        }
        return texto.toString();
    }

    /** Claves repetidas en el mismo mapa (misma ruta y misma sangria). */
    public static List<String> clavesDuplicadas(File archivo) {
        return clavesDuplicadas(leerTexto(archivo));
    }

    /** Claves repetidas en el mismo mapa (misma ruta y misma sangria). */
    public static List<String> clavesDuplicadas(String texto) {
        List<String> repetidas = new ArrayList<String>();
        if (texto == null || texto.isEmpty()) {
            return repetidas;
        }
        Map<String, Integer> vistas = new LinkedHashMap<String, Integer>();
        String[] porSangria = new String[64];
        int[] elementos = new int[64];
        String[] lineas = texto.split("\\r?\\n");
        for (String linea : lineas) {
            String limpia = linea.trim();
            if (limpia.isEmpty() || limpia.startsWith("#")) {
                continue;
            }
            int sangriaLista = sangriaDeLista(linea);
            if (sangriaLista >= 0) {
                // Nuevo elemento de lista: lo que venga detras es otro mapa.
                if (sangriaLista < porSangria.length) {
                    elementos[sangriaLista]++;
                    for (int i = sangriaLista + 1; i < porSangria.length; i++) {
                        porSangria[i] = null;
                        elementos[i] = 0;
                    }
                }
                continue;
            }
            Matcher buscador = CLAVE.matcher(linea);
            if (!buscador.matches()) {
                continue;
            }
            int sangria = buscador.group(1).length();
            String clave = limpiar(buscador.group(2));
            if (clave.isEmpty()) {
                continue;
            }
            StringBuilder ruta = new StringBuilder();
            for (int i = 0; i < sangria && i < porSangria.length; i++) {
                if (porSangria[i] != null) {
                    ruta.append(ruta.length() > 0 ? "." : "").append(porSangria[i]);
                }
                if (elementos[i] > 0) {
                    ruta.append(ruta.length() > 0 ? "." : "").append('#').append(elementos[i]);
                }
            }
            ruta.append(ruta.length() > 0 ? "." : "").append(clave);
            String completa = ruta.toString();
            Integer veces = vistas.get(completa);
            if (veces == null) {
                vistas.put(completa, Integer.valueOf(1));
            } else {
                vistas.put(completa, Integer.valueOf(veces.intValue() + 1));
                if (!repetidas.contains(completa)) {
                    repetidas.add(completa);
                }
            }
            if (sangria < porSangria.length) {
                porSangria[sangria] = clave;
                for (int i = sangria + 1; i < porSangria.length; i++) {
                    porSangria[i] = null;
                    elementos[i] = 0;
                }
            }
        }
        return repetidas;
    }

    /** Sangria de una linea de lista ("- ..."), o -1 si no lo es. */
    private static int sangriaDeLista(String linea) {
        int i = 0;
        while (i < linea.length() && linea.charAt(i) == ' ') {
            i++;
        }
        if (i >= linea.length() || linea.charAt(i) != '-') {
            return -1;
        }
        if (i + 1 < linea.length()) {
            char siguiente = linea.charAt(i + 1);
            if (siguiente != ' ' && siguiente != '\t') {
                return -1;
            }
        }
        return i;
    }

    private static String limpiar(String clave) {
        String limpia = clave == null ? "" : clave.trim();
        if (limpia.length() >= 2) {
            char primera = limpia.charAt(0);
            char ultima = limpia.charAt(limpia.length() - 1);
            if ((primera == '\'' && ultima == '\'') || (primera == '"' && ultima == '"')) {
                limpia = limpia.substring(1, limpia.length() - 1).trim();
            }
        }
        return limpia;
    }
}
