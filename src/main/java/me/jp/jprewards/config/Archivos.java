package me.jp.jprewards.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gestor de archivos de configuracion.
 *
 * Se encarga de tres cosas que hacen que actualizar el plugin no de miedo:
 *
 *  1. CREAR lo que falta desde el propio .jar (sin pisar nada existente).
 *  2. ACTUALIZAR: cuando una version nueva agrega opciones, se insertan en
 *     el archivo del usuario CON sus comentarios y en su sitio, respetando
 *     todo lo que el usuario ya tenia configurado. Antes de tocar nada se
 *     guarda una copia .bak.
 *  3. LEER en UTF-8 siempre (en servidores con Windows el YAML se puede
 *     corromper si se lee con la codificacion del sistema).
 *
 * El formato de las lineas se analiza a mano (no se reescribe el YAML con
 * SnakeYAML) justamente para no perder los comentarios del usuario.
 */
public final class Archivos {

    /** Version del formato de los archivos que entiende esta build. */
    public static final int CONFIG_VERSION = 2;

    /**
     * Archivos que el plugin crea y mantiene actualizados.
     *
     * Los que acaban en '_es' son los PRESETS en espanol: vienen listos para
     * usar y se activan cambiando el archivo correspondiente en config.yml
     * (menus.archivo, diaria.archivo...). Se crean para que pasar el plugin a
     * espanol sea cambiar cuatro lineas, no traducir trece archivos.
     *
     * 'recompensas/extra.yml' son los premios sueltos (los entrega la API o
     * /rewards trigger), no un preset: viene vacio y es opcional.
     */
    private static final String[] GESTIONADOS = {
            "config.yml",
            "messages_es.yml",
            "messages_en.yml",
            "menus.yml",
            "menus_es.yml",
            "recompensas/diarias.yml",
            "recompensas/diarias_es.yml",
            "recompensas/semanales.yml",
            "recompensas/semanales_es.yml",
            "recompensas/mensuales.yml",
            "recompensas/mensuales_es.yml",
            "recompensas/rachas.yml",
            "recompensas/rachas_es.yml",
            "recompensas/extra.yml"
    };

    /** Los mismos archivos, para el resumen de inicio. */
    public static String[] gestionados() {
        return GESTIONADOS.clone();
    }

    private static final Pattern CLAVE = Pattern.compile("^( *)([^#\\s][^:]*):(.*)$");
    private static final Pattern VERSION = Pattern.compile("^\\s*config-version\\s*:.*$", Pattern.MULTILINE);

    private final File carpeta;
    private final Logger registro;
    private int agregadas;
    private boolean primeraVez;

    public Archivos(File carpeta, Logger registro) {
        this.carpeta = carpeta;
        this.registro = registro;
    }

    // ------------------------------------------------------------------
    // Rutas
    // ------------------------------------------------------------------

    public File carpeta() {
        return carpeta;
    }

    public File archivo(String ruta) {
        return new File(carpeta, ruta);
    }

    public boolean existe(String ruta) {
        return archivo(ruta).isFile();
    }

    /** Cuantas opciones nuevas se agregaron en la ultima actualizacion. */
    public int opcionesAgregadas() {
        return agregadas;
    }

    /** true si config.yml no existia y se acaba de crear (primera instalacion). */
    public boolean primeraVez() {
        return primeraVez;
    }

    // ------------------------------------------------------------------
    // 1. Crear lo que falta
    // ------------------------------------------------------------------

    /**
     * Crea en la carpeta del plugin los archivos que falten, copiandolos del
     * .jar tal cual (con sus comentarios). Si el usuario tenia un
     * messages.yml antiguo (version 1.x) se renombra a messages_es.yml para
     * no perder su traduccion.
     */
    public void crearPorDefecto(org.bukkit.plugin.Plugin plugin) {
        primeraVez = !archivo("config.yml").isFile();
        File antiguo = archivo("messages.yml");
        File nuevo = archivo("messages_es.yml");
        boolean desdeVersion1 = false;
        if (antiguo.isFile() && !nuevo.isFile()) {
            if (antiguo.renameTo(nuevo)) {
                registro.info("messages.yml was renamed to messages_es.yml (same content, new format).");
                desdeVersion1 = true;
            } else {
                registro.warning("Could not rename messages.yml; check the plugin folder permissions.");
            }
        }
        for (String ruta : GESTIONADOS) {
            File destino = archivo(ruta);
            if (destino.isFile()) {
                continue;
            }
            try {
                plugin.saveResource(ruta, false);
            } catch (IllegalArgumentException sinRecurso) {
                registro.warning("The file '" + ruta + "' does not exist inside the .jar (incomplete build).");
            }
        }
        if (desdeVersion1) {
            conservarIdiomaEspanol();
        }
    }

    /**
     * Al actualizar desde la 1.x (que solo existia en espanol) se deja el
     * idioma en 'es' si el config.yml todavia no lo define, para que el chat no
     * cambie de idioma solo al actualizar el plugin. En una instalacion nueva
     * el idioma por defecto es el ingles.
     */
    private void conservarIdiomaEspanol() {
        File config = archivo("config.yml");
        if (!config.isFile()) {
            return;
        }
        try {
            String texto = leer(config);
            if (Pattern.compile("^\\s*idioma\\s*:", Pattern.MULTILINE).matcher(texto).find()) {
                return;
            }
            String linea = System.lineSeparator() + "# Language kept from version 1.x (only Spanish existed back then)."
                    + System.lineSeparator() + "idioma: es";
            Matcher version = VERSION.matcher(texto);
            if (version.find()) {
                texto = texto.substring(0, version.end()) + linea + texto.substring(version.end());
            } else {
                texto = "idioma: es" + System.lineSeparator() + texto;
            }
            escribir(config, texto);
            registro.info("config.yml: added 'idioma: es' because you are upgrading from 1.x (so the chat stays in Spanish).");
        } catch (IOException error) {
            registro.warning("Could not keep the language in config.yml: " + error.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 2. Actualizar (fusionar opciones nuevas)
    // ------------------------------------------------------------------

    /**
     * Agrega al archivo del usuario las opciones que existen en el archivo
     * incluido en el .jar y que aun no tiene, conservando comentarios y
     * valores. Devuelve la lista de rutas agregadas (vacia si ya estaba al dia).
     */
    public List<String> actualizar(String ruta, int versionObjetivo) {
        List<String> agregadasAhora = new ArrayList<String>();
        File destino = archivo(ruta);
        if (!destino.isFile()) {
            return agregadasAhora;
        }
        String defecto = leerRecurso(ruta);
        if (defecto == null || defecto.isEmpty()) {
            return agregadasAhora;
        }
        String usuario;
        try {
            usuario = leer(destino);
        } catch (IOException error) {
            registro.warning("Could not read '" + ruta + "': " + error.getMessage());
            return agregadasAhora;
        }
        YamlConfiguration ymlUsuario = cargarTexto(usuario);
        YamlConfiguration ymlDefecto = cargarTexto(defecto);
        if (ymlUsuario == null || ymlDefecto == null) {
            return agregadasAhora;
        }

        List<String> lineasUsuario = lineas(usuario);
        List<String> lineasDefecto = lineas(defecto);

        fusionar(lineasUsuario, lineasDefecto, 0, -1, "", null, agregadasAhora);

        boolean cambio = !agregadasAhora.isEmpty();
        if (agregadasAhora.isEmpty()) {
            // Igual hay que subir config-version: eso tambien es un cambio.
            int versionActual = ymlUsuario.getInt("config-version", versionObjetivo);
            if (versionActual != versionObjetivo) {
                cambio = true;
            }
        }
        if (!cambio) {
            return agregadasAhora;
        }

        copiaDeSeguridad(destino);

        String texto = unir(lineasUsuario);
        texto = VERSION.matcher(texto).replaceFirst("config-version: " + versionObjetivo);
        if (!texto.contains("config-version")) {
            texto = "config-version: " + versionObjetivo + System.lineSeparator() + texto;
        }
        try {
            escribir(destino, texto);
        } catch (IOException error) {
            registro.warning("Could not update '" + ruta + "': " + error.getMessage());
            return new ArrayList<String>();
        }
        agregadas += agregadasAhora.size();
        return agregadasAhora;
    }

    /**
     * Fusiona recursivamente: por cada clave del archivo por defecto que no
     * este en el del usuario, inserta su bloque (comentarios incluidos) al
     * final de la seccion correspondiente.
     */
    private void fusionar(List<String> lineasUsuario, List<String> lineasDefecto, int indentacion,
            int indentacionPadre, String rutaPadre, String clavePadre, List<String> agregadas) {
        for (Bloque bloque : bloques(lineasDefecto, indentacion)) {
            String ruta = rutaPadre.isEmpty() ? bloque.clave : rutaPadre + "." + bloque.clave;
            int[] rango = rangoSeccion(lineasUsuario, bloque.clave, indentacion);
            int hijos = indentacionHijos(bloque.lineas);
            if (rango != null) {
                // La clave ya existe: se revisan SOLO sus hijos, dentro de esa seccion.
                if (hijos > indentacion) {
                    // subList es una vista: lo que se inserte ahi va al archivo real.
                    fusionar(lineasUsuario.subList(rango[0], rango[1]), bloque.lineas, hijos, indentacion, ruta,
                            bloque.clave, agregadas);
                }
                continue;
            }
            // No existe: se inserta el bloque entero con sus comentarios.
            insertarBloque(lineasUsuario, clavePadre, indentacionPadre, bloque, ruta, agregadas);
        }
    }

    /**
     * Indentacion de los hijos de un bloque (la del primer hijo real), o -1 si
     * no tiene. Sin esto, las opciones nuevas dentro de una seccion existente
     * no se encontraban y acababan fuera de su sitio.
     */
    private static int indentacionHijos(List<String> lineas) {
        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
                continue;
            }
            return sangria(linea);
        }
        return -1;
    }

    /** Rango [inicio, fin) de la seccion 'clave' dentro de estas lineas, o null. */
    private int[] rangoSeccion(List<String> lineas, String clave, int indentacion) {
        for (int i = 0; i < lineas.size(); i++) {
            Matcher buscador = CLAVE.matcher(lineas.get(i));
            if (buscador.matches() && buscador.group(1).length() == indentacion
                    && normalizarClave(buscador.group(2)).equals(clave)) {
                int fin = finDeSeccion(lineas, clave, indentacion);
                return new int[]{i, Math.max(fin, i + 1)};
            }
        }
        return null;
    }

    private void insertarBloque(List<String> destino, String clavePadre, int indentPadre, Bloque bloque,
            String ruta, List<String> agregadas) {
        StringBuilder sangria = new StringBuilder();
        for (int i = 0; i < Math.max(0, bloque.indentacion); i++) {
            sangria.append(' ');
        }
        List<String> insercion = new ArrayList<String>();
        insercion.add("");
        insercion.add(sangria + "# --- NEW in this version: " + ruta + " (check this option) ---");
        for (String comentario : bloque.comentarios) {
            insercion.add(comentario.trim().isEmpty() ? "" : sangria + comentario.trim());
        }
        insercion.addAll(bloque.lineas);
        if (clavePadre == null || clavePadre.isEmpty()) {
            destino.addAll(insercion);
        } else {
            int posicion = finDeSeccion(destino, clavePadre, indentPadre);
            destino.addAll(posicion, insercion);
        }
        agregadas.add(ruta);
    }

    /** Indice donde termina la seccion 'clave' (para insertar justo antes). */
    private int finDeSeccion(List<String> lineas, String clave, int indentacion) {
        int inicio = -1;
        for (int i = 0; i < lineas.size(); i++) {
            Matcher buscador = CLAVE.matcher(lineas.get(i));
            if (buscador.matches() && buscador.group(1).length() == indentacion
                    && normalizarClave(buscador.group(2)).equals(clave)) {
                inicio = i;
                break;
            }
        }
        if (inicio < 0) {
            return lineas.size();
        }
        int i = inicio + 1;
        int fin = lineas.size();
        for (; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (linea.trim().isEmpty()) {
                continue;
            }
            int sangria = sangria(linea);
            if (sangria <= indentacion) {
                fin = i;
                break;
            }
        }
        // Retrocede sobre lineas en blanco para no dejar huecos raros.
        while (fin > inicio + 1 && lineas.get(fin - 1).trim().isEmpty()) {
            fin--;
        }
        return fin;
    }

    /**
     * Divide un archivo (o una seccion) en bloques: cada clave con sus
     * comentarios de arriba y todas sus lineas hijas.
     */
    private List<Bloque> bloques(List<String> lineas, int nivel) {
        List<Bloque> bloques = new ArrayList<Bloque>();
        List<String> comentarios = new ArrayList<String>();
        Bloque actual = null;
        for (String linea : lineas) {
            if (linea.trim().isEmpty()) {
                if (actual != null) {
                    actual.lineas.add(linea);
                } else {
                    comentarios.clear();
                }
                continue;
            }
            if (linea.trim().startsWith("#")) {
                if (actual != null) {
                    actual.lineas.add(linea);
                } else {
                    comentarios.add(linea);
                }
                continue;
            }
            int sangria = sangria(linea);
            if (sangria < nivel) {
                if (actual != null) {
                    bloques.add(actual);
                    actual = null;
                }
                comentarios.clear();
                continue;
            }
            Matcher buscador = CLAVE.matcher(linea);
            if (sangria == nivel && buscador.matches()) {
                if (actual != null) {
                    bloques.add(actual);
                }
                actual = new Bloque();
                actual.clave = normalizarClave(buscador.group(2));
                actual.indentacion = nivel;
                actual.comentarios = new ArrayList<String>(comentarios);
                actual.lineas = new ArrayList<String>();
                actual.lineas.add(linea);
                comentarios.clear();
                continue;
            }
            if (actual != null) {
                actual.lineas.add(linea);
            }
        }
        if (actual != null) {
            bloques.add(actual);
        }
        return bloques;
    }

    /** Un bloque de YAML: clave, comentarios de arriba y lineas propias. */
    private static final class Bloque {
        private String clave;
        private int indentacion;
        private List<String> comentarios = new ArrayList<String>();
        private List<String> lineas = new ArrayList<String>();
    }

    /** Normaliza una clave: quita comillas y espacios sobrantes. */
    private static String normalizarClave(String clave) {
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

    // ------------------------------------------------------------------
    // 3. Lectura y escritura en UTF-8
    // ------------------------------------------------------------------

    /** Carga un YAML de la carpeta del plugin (creandolo si falta) en UTF-8. */
    public YamlConfiguration cargar(String ruta) {
        File destino = archivo(ruta);
        if (!destino.isFile()) {
            return null;
        }
        try (Reader lector = new InputStreamReader(new FileInputStream(destino), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(lector);
        } catch (IOException error) {
            registro.warning("Could not read '" + ruta + "': " + error.getMessage());
            return null;
        }
    }

    /** Carga el archivo incluido en el .jar (por defecto) en UTF-8. */
    public YamlConfiguration cargarRecurso(String ruta) {
        String texto = leerRecurso(ruta);
        return texto == null ? null : cargarTexto(texto);
    }

    private YamlConfiguration cargarTexto(String texto) {
        try {
            return YamlConfiguration.loadConfiguration(new java.io.StringReader(texto));
        } catch (Throwable error) {
            registro.warning("Invalid YAML: " + error.getMessage());
            return null;
        }
    }

    /** Texto completo del archivo incluido en el .jar, o null. */
    public String leerRecurso(String ruta) {
        try (InputStream entrada = getClass().getClassLoader().getResourceAsStream(ruta)) {
            if (entrada == null) {
                return null;
            }
            return leer(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        } catch (IOException error) {
            return null;
        }
    }

    public String leer(File archivo) throws IOException {
        try (Reader lector = new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8)) {
            return leer(lector);
        }
    }

    private static String leer(Reader lector) throws IOException {
        StringBuilder texto = new StringBuilder();
        char[] buffer = new char[8192];
        int leidos;
        while ((leidos = lector.read(buffer)) > 0) {
            texto.append(buffer, 0, leidos);
        }
        return texto.toString();
    }

    public void escribir(File archivo, String texto) throws IOException {
        File padre = archivo.getParentFile();
        if (padre != null && !padre.isDirectory() && !padre.mkdirs()) {
            throw new IOException("Could not create the folder " + padre.getPath());
        }
        try (Writer escritor = new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8)) {
            escritor.write(texto);
        }
    }

    /** Copia el archivo a <nombre>.bak antes de modificarlo. */
    public File copiaDeSeguridad(File archivo) {
        File copia = new File(archivo.getParentFile(), archivo.getName() + ".bak");
        try {
            escribir(copia, leer(archivo));
            return copia;
        } catch (IOException error) {
            registro.warning("Could not save the backup of " + archivo.getName() + ": " + error.getMessage());
            return null;
        }
    }

    /** Copia con fecha, para guardar el estado de los datos antes de migrar. */
    public File copiaConFecha(File archivo) {
        String sello = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File copia = new File(archivo.getParentFile(), archivo.getName() + ".bak-" + sello);
        try {
            escribir(copia, leer(archivo));
            return copia;
        } catch (IOException error) {
            return null;
        }
    }

    public static List<String> lineas(String texto) {
        List<String> lineas = new ArrayList<String>();
        String normalizado = texto.replace("\r\n", "\n").replace('\r', '\n');
        int inicio = 0;
        for (int i = 0; i < normalizado.length(); i++) {
            if (normalizado.charAt(i) == '\n') {
                lineas.add(normalizado.substring(inicio, i));
                inicio = i + 1;
            }
        }
        lineas.add(normalizado.substring(inicio));
        return lineas;
    }

    public static String unir(List<String> lineas) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < lineas.size(); i++) {
            texto.append(lineas.get(i));
            if (i < lineas.size() - 1) {
                texto.append(System.lineSeparator());
            }
        }
        return texto.toString();
    }

    private static int sangria(String linea) {
        int espacios = 0;
        while (espacios < linea.length() && linea.charAt(espacios) == ' ') {
            espacios++;
        }
        return espacios;
    }
}
