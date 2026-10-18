package me.jp.jprewards.config;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.hook.PuentePlaceholders;
import me.jp.jprewards.util.Texto;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Todos los textos del plugin viven en messages_<idioma>.yml: aqui se
 * reemplazan los placeholders {asi}, se aplican los de PlaceholderAPI (si
 * esta instalado) y se envia.
 *
 * Detalles pensados para que configurar sea agradable:
 *   - Si falta una clave se usa el texto del archivo ingles incluido en el
 *     .jar, asi una traduccion incompleta nunca deja huecos.
 *   - Un mensaje vacio NO se envia (utilidad: dejar una clave en "" para
 *     silenciarla sin borrarla).
 *   - {prefijo} se puede usar dentro de cualquier mensaje; si no aparece, el
 *     prefijo se agrega delante automaticamente.
 *   - Las claves que falten se listan en /rewards validate.
 */
public final class Mensajes {

    /** Idioma de reserva: siempre viene dentro del .jar. */
    public static final String IDIOMA_POR_DEFECTO = "en";

    /** Solo letras, numeros, guion y guion bajo: evita rutas como ../config. */
    private static final Pattern CODIGO_IDIOMA = Pattern.compile("[a-z0-9_-]{2,16}");

    private final JPRewards plugin;
    private final Set<String> clavesAvisadas = new HashSet<String>();

    private FileConfiguration archivo;
    private FileConfiguration reserva;
    private String idioma = IDIOMA_POR_DEFECTO;
    private String prefijo = "";

    public Mensajes(JPRewards plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Carga
    // ------------------------------------------------------------------

    /** Carga (o recarga) el idioma pedido. Nunca lanza. */
    public void cargar(String pedido) {
        idioma = normalizar(pedido);
        if (pedido != null && !pedido.trim().equalsIgnoreCase(idioma)) {
            plugin.getLogger().warning("The language '" + pedido + "' in config.yml is not valid; using '" + idioma + "'.");
        }
        reserva = plugin.archivos().cargarRecurso("messages_" + IDIOMA_POR_DEFECTO + ".yml");
        clavesAvisadas.clear();

        String ruta = "messages_" + idioma + ".yml";
        if (!plugin.archivos().existe(ruta)) {
            if (IDIOMA_POR_DEFECTO.equals(idioma)) {
                plugin.getLogger().warning("Could not find " + ruta + " in the plugin folder.");
            } else {
                plugin.getLogger().warning("There is no " + ruta + " (idioma '" + idioma
                        + "'). The texts bundled in the .jar will be used, and you can create the file yourself.");
            }
        }
        archivo = plugin.archivos().cargar(ruta);
        prefijo = Texto.color(valor("prefijo", ""));
    }

    private static String normalizar(String idioma) {
        if (idioma == null) {
            return IDIOMA_POR_DEFECTO;
        }
        String limpio = idioma.trim().toLowerCase(Locale.ROOT);
        return CODIGO_IDIOMA.matcher(limpio).matches() ? limpio : IDIOMA_POR_DEFECTO;
    }

    public String idioma() {
        return idioma;
    }

    public String prefijo() {
        return prefijo;
    }

    /** Archivo cargado (para el validador). */
    public FileConfiguration archivo() {
        return archivo;
    }

    private String valor(String ruta, String porDefecto) {
        String texto = archivo == null ? null : archivo.getString(ruta);
        if (texto == null && reserva != null) {
            texto = reserva.getString(ruta);
        }
        if (texto == null) {
            avisarFalta(ruta);
            return porDefecto;
        }
        return texto;
    }

    // ------------------------------------------------------------------
    // Texto
    // ------------------------------------------------------------------

    /** Texto coloreado (sin prefijo), con {prefijo} resuelto. */
    public String texto(String ruta) {
        return texto(ruta, null, null);
    }

    public String texto(String ruta, Map<String, String> placeholders) {
        return texto(ruta, placeholders, null);
    }

    /** Texto coloreado; si hay jugador se aplican tambien sus placeholders de PlaceholderAPI. */
    public String texto(String ruta, Map<String, String> placeholders, Player jugador) {
        String bruto = valor(ruta, "");
        if (bruto.isEmpty()) {
            return "";
        }
        String conPrefijo = bruto.replace("{prefijo}", prefijo);
        String texto = Texto.color(Texto.reemplazar(conPrefijo, placeholders));
        return jugador == null ? texto : PuentePlaceholders.aplicar(jugador, texto);
    }

    /** Lista de textos coloreados (vacia si la clave no existe o esta vacia). */
    public List<String> lista(String ruta, Map<String, String> placeholders) {
        List<String> valores = archivo == null ? null : archivo.getStringList(ruta);
        if ((valores == null || valores.isEmpty()) && reserva != null) {
            valores = reserva.getStringList(ruta);
        }
        if (valores == null || valores.isEmpty()) {
            return new ArrayList<String>();
        }
        List<String> salida = new ArrayList<String>(valores.size());
        for (String linea : valores) {
            String conPrefijo = linea.replace("{prefijo}", prefijo);
            salida.add(Texto.color(Texto.reemplazar(conPrefijo, placeholders)));
        }
        return salida;
    }

    /** true si la clave existe y tiene algo visible. */
    public boolean existeVisible(String ruta) {
        String bruto = valor(ruta, "");
        return !Texto.vacioVisible(bruto);
    }

    // ------------------------------------------------------------------
    // Envio
    // ------------------------------------------------------------------

    /** Envia un mensaje con el prefijo (o con {prefijo} en su sitio). */
    public void enviar(CommandSender destino, String ruta) {
        enviar(destino, ruta, (Map<String, String>) null);
    }

    public void enviar(CommandSender destino, String ruta, Map<String, String> placeholders) {
        if (destino == null) {
            return;
        }
        String bruto = valor(ruta, "");
        if (Texto.vacioVisible(bruto)) {
            return;
        }
        String conPlaceholders = Texto.reemplazar(bruto, placeholders);
        String final_ = bruto.contains("{prefijo}") ? conPlaceholders.replace("{prefijo}", prefijo)
                : prefijo + conPlaceholders;
        String texto = Texto.color(final_);
        if (destino instanceof Player) {
            texto = PuentePlaceholders.aplicar((Player) destino, texto);
        }
        destino.sendMessage(texto);
    }

    /** Envia un mensaje a un jugador usando pares clave/valor. */
    public void enviar(CommandSender destino, String ruta, String... pares) {
        enviar(destino, ruta, mapa(pares));
    }

    /** Envia todas las lineas de una lista (con prefijo en cada una). */
    public void enviarLista(CommandSender destino, String ruta, Map<String, String> placeholders) {
        for (String linea : lista(ruta, placeholders)) {
            if (Texto.vacioVisible(linea)) {
                destino.sendMessage("");
                continue;
            }
            String texto = linea.contains("{prefijo}") ? linea : prefijo + linea;
            if (destino instanceof Player) {
                texto = PuentePlaceholders.aplicar((Player) destino, texto);
            }
            destino.sendMessage(texto);
        }
    }

    public void enviarLista(CommandSender destino, String ruta, String... pares) {
        enviarLista(destino, ruta, mapa(pares));
    }

    /**
     * Envia las lineas de una lista TAL CUAL (sin prefijo): se usa para las
     * ayudas y los cuadros de informacion, donde el prefijo en cada linea
     * solo estorba. Una linea vacia se envia como linea en blanco.
     */
    public void enviarLineas(CommandSender destino, String ruta, Map<String, String> placeholders) {
        if (destino == null) {
            return;
        }
        for (String linea : lista(ruta, placeholders)) {
            if (linea.trim().isEmpty()) {
                destino.sendMessage("");
                continue;
            }
            if (destino instanceof Player) {
                destino.sendMessage(PuentePlaceholders.aplicar((Player) destino, linea));
            } else {
                destino.sendMessage(linea);
            }
        }
    }

    /** Envia un texto ya montado (sin buscarlo en el archivo). */
    public void enviarCrudo(CommandSender destino, String texto) {
        if (destino == null || Texto.vacioVisible(texto)) {
            return;
        }
        destino.sendMessage(prefijo + Texto.color(texto));
    }

    // ------------------------------------------------------------------
    // Utilidades para el validador
    // ------------------------------------------------------------------

    /** Claves presentes en el archivo incluido en el .jar y ausentes en el del usuario. */
    public List<String> clavesFaltantes() {
        List<String> faltantes = new ArrayList<String>();
        if (reserva == null || archivo == null) {
            return faltantes;
        }
        for (String clave : recorrer(reserva, "")) {
            if (!archivo.isSet(clave)) {
                faltantes.add(clave);
            }
        }
        return faltantes;
    }

    /** Claves del archivo del usuario que ya no existen en la version actual. */
    public List<String> clavesObsoletas() {
        List<String> obsoletas = new ArrayList<String>();
        if (reserva == null || archivo == null) {
            return obsoletas;
        }
        for (String clave : recorrer(archivo, "")) {
            if (!reserva.isSet(clave)) {
                obsoletas.add(clave);
            }
        }
        return obsoletas;
    }

    /** Rutas de todas las claves de una configuracion (recursivo). */
    public static List<String> recorrer(FileConfiguration configuracion, String prefijo) {
        List<String> claves = new ArrayList<String>();
        if (configuracion == null) {
            return claves;
        }
        Set<String> directas = configuracion.getKeys(false);
        for (String clave : directas) {
            String ruta = prefijo.isEmpty() ? clave : prefijo + "." + clave;
            Object valor = configuracion.get(clave);
            if (valor instanceof ConfigurationSection) {
                claves.addAll(recorrer((ConfigurationSection) valor, ruta));
            } else {
                claves.add(ruta);
            }
        }
        return claves;
    }

    private static List<String> recorrer(ConfigurationSection seccion, String prefijo) {
        List<String> claves = new ArrayList<String>();
        if (seccion == null) {
            return claves;
        }
        for (String clave : seccion.getKeys(false)) {
            String ruta = prefijo.isEmpty() ? clave : prefijo + "." + clave;
            Object valor = seccion.get(clave);
            if (valor instanceof ConfigurationSection) {
                claves.addAll(recorrer((ConfigurationSection) valor, ruta));
            } else {
                claves.add(ruta);
            }
        }
        return claves;
    }

    private void avisarFalta(String ruta) {
        if (clavesAvisadas.add(ruta)) {
            plugin.getLogger().warning("Missing key '" + ruta + "' en messages_" + idioma
                    + ".yml (nor is it in the file bundled in the .jar).");
        }
    }

    // ------------------------------------------------------------------
    // Mapas de placeholders
    // ------------------------------------------------------------------

    /** Atajo: ph("clave", valor) -> mapa de placeholders. */
    public static Map<String, String> ph(String clave, String valor) {
        Map<String, String> mapa = new LinkedHashMap<String, String>();
        mapa.put(clave, valor);
        return mapa;
    }

    public static Map<String, String> ph(String clave1, String valor1, String clave2, String valor2) {
        Map<String, String> mapa = ph(clave1, valor1);
        mapa.put(clave2, valor2);
        return mapa;
    }

    public static Map<String, String> ph(String clave1, String valor1, String clave2, String valor2, String clave3, String valor3) {
        Map<String, String> mapa = ph(clave1, valor1, clave2, valor2);
        mapa.put(clave3, valor3);
        return mapa;
    }

    public static Map<String, String> ph(String clave1, String valor1, String clave2, String valor2, String clave3, String valor3,
            String clave4, String valor4) {
        Map<String, String> mapa = ph(clave1, valor1, clave2, valor2, clave3, valor3);
        mapa.put(clave4, valor4);
        return mapa;
    }

    /** Convierte pares ("clave", "valor", ...) en un mapa. */
    public static Map<String, String> mapa(String... pares) {
        Map<String, String> mapa = new LinkedHashMap<String, String>();
        if (pares == null) {
            return mapa;
        }
        for (int i = 0; i + 1 < pares.length; i += 2) {
            mapa.put(pares[i], pares[i + 1]);
        }
        return mapa;
    }

    /** Copia inmutable de un mapa (para no exponer el original). */
    public static Map<String, String> copia(Map<String, String> original) {
        return original == null ? Collections.<String, String>emptyMap()
                : new LinkedHashMap<String, String>(original);
    }
}
