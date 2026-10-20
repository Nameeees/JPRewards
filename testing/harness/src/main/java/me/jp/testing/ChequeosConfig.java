package me.jp.testing;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Comprobacion 8: fusion de configuracion (Archivos.actualizar).
 *
 * Se crea un config.yml de juguete con una opcion conocida cambiada y sin
 * ninguna seccion, se ejecuta la fusion contra el config.yml que va dentro del
 * jar y se comprueba que:
 *   (a) el valor del usuario se respeta,
 *   (b) las secciones (y su primer hijo) que faltaban aparecen,
 *   (c) el archivo sigue siendo YAML valido, sin claves duplicadas y con
 *       config-version subida a 2.
 */
public final class ChequeosConfig {

    private final Chequeos base;
    private final Registro registro;
    private final Logger log;

    public ChequeosConfig(Chequeos base, Registro registro, Logger log) {
        this.base = base;
        this.registro = registro;
        this.log = log;
    }

    public void fusionConfiguracion() {
        if (!base.activo()) {
            registro.saltado("fusion de config: JPRewards no esta cargado y activo");
            return;
        }
        Class<?> claseArchivos = Reflexion.clase(base.cargador(), "me.jp.jprewards.config.Archivos");
        if (claseArchivos == null) {
            registro.saltado("fusion de config: no existe aun la clase me.jp.jprewards.config.Archivos");
            return;
        }
        File carpeta = new File(base.jprewards().getDataFolder(), "harness-merge");
        try {
            ChequeosDatos.borrar(carpeta);
            if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
                registro.fallo("fusion de config: no se pudo crear la carpeta temporal " + carpeta.getPath());
                return;
            }
            Method leerRecurso = Reflexion.metodo(claseArchivos, "leerRecurso", String.class);
            Method actualizar = Reflexion.metodo(claseArchivos, "actualizar", String.class, int.class);
            if (leerRecurso == null || actualizar == null) {
                registro.saltado("fusion de config: faltan los metodos leerRecurso(String)/actualizar(String, int)");
                return;
            }
            Object archivos = Reflexion.nuevo(claseArchivos,
                    new Class<?>[] { File.class, Logger.class },
                    new Object[] { carpeta, log });
            Object recurso = Reflexion.invocar(leerRecurso, archivos, "config.yml");
            if (!(recurso instanceof String) || ((String) recurso).trim().isEmpty()) {
                registro.saltado("fusion de config: el jar de JPRewards no trae config.yml");
                return;
            }
            YamlConfiguration porDefecto = YamlConfiguration.loadConfiguration(new StringReader((String) recurso));
            Object[] valorElegido = new Object[1];
            String rutaEscalar = buscarEscalar(porDefecto, "", valorElegido);
            if (rutaEscalar == null) {
                registro.saltado("fusion de config: el config.yml por defecto no tiene ninguna opcion escalar para probar");
                return;
            }
            List<String> secciones = new ArrayList<String>();
            Map<String, String> primerHijo = new LinkedHashMap<String, String>();
            for (String clave : porDefecto.getKeys(false)) {
                if (!porDefecto.isConfigurationSection(clave)) {
                    continue;
                }
                secciones.add(clave);
                ConfigurationSection seccion = porDefecto.getConfigurationSection(clave);
                for (String hijo : seccion.getKeys(false)) {
                    primerHijo.put(clave, hijo);
                    break;
                }
            }
            if (secciones.isEmpty()) {
                registro.saltado("fusion de config: el config.yml por defecto no tiene secciones para probar");
                return;
            }
            Object valorJuguete = valorDePrueba(valorElegido[0]);
            StringBuilder juguete = new StringBuilder();
            juguete.append("config-version: 1").append('\n').append('\n');
            juguete.append("# config.yml de juguete del harness (no es el del plugin)\n");
            escribirRuta(juguete, rutaEscalar, valorJuguete);
            File destino = new File(carpeta, "config.yml");
            Files.write(destino.toPath(), juguete.toString().getBytes(StandardCharsets.UTF_8));
            registro.info("[harness] fusion: opcion del usuario " + rutaEscalar + " = " + valorJuguete);
            Reflexion.invocar(actualizar, archivos, "config.yml", Integer.valueOf(2));
            YamlConfiguration despues;
            try {
                despues = YamlConfiguration.loadConfiguration(destino);
            } catch (Throwable error) {
                registro.fallo("fusion de config: el YAML resultante no se puede leer (" + Registro.describir(error) + ")");
                return;
            }
            List<String> problemas = new ArrayList<String>();
            // (a) el valor del usuario se respeta
            Object valorFinal = despues.get(rutaEscalar);
            if (!Comparador.iguales(valorFinal, valorJuguete)) {
                problemas.add("no se respeto " + rutaEscalar + " (usuario=" + valorJuguete + ", quedo=" + valorFinal + ")");
            }
            // (b) las secciones que faltaban aparecen (con su primer hijo)
            List<String> ausentes = new ArrayList<String>();
            for (String seccion : secciones) {
                if (!despues.isConfigurationSection(seccion)) {
                    ausentes.add(seccion);
                    continue;
                }
                String hijo = primerHijo.get(seccion);
                if (hijo != null && !despues.isSet(seccion + "." + hijo)) {
                    ausentes.add(seccion + "." + hijo);
                }
            }
            if (!ausentes.isEmpty()) {
                problemas.add("no se agregaron: " + ausentes);
            }
            // (c) config-version subida y YAML valido sin duplicados
            long version = despues.getLong("config-version");
            if (version < 2L) {
                problemas.add("config-version=" + version + " (se esperaba 2)");
            }
            List<String> duplicadas = YamlUtil.clavesDuplicadas(destino);
            if (!duplicadas.isEmpty()) {
                problemas.add("claves duplicadas: " + duplicadas);
            }
            boolean copia = new File(carpeta, "config.yml.bak").isFile();
            String textoFinal = YamlUtil.leerTexto(destino).replace("\r", "").replace("\n", " | ");
            registro.info("[harness] fusion: resultado (" + textoFinal.length() + " chars) " + recortar(textoFinal, 600));
            if (problemas.isEmpty()) {
                registro.ok("fusion de config: se respeto " + rutaEscalar + "=" + valorJuguete + ", aparecieron "
                        + secciones.size() + " secciones, config-version=2, YAML valido sin claves duplicadas (copia .bak: "
                        + (copia ? "si" : "no") + ")");
            } else {
                registro.fallo("fusion de config: " + problemas);
            }
        } catch (Throwable error) {
            registro.fallo("fusion de config: " + Registro.describir(error));
        } finally {
            ChequeosDatos.borrar(carpeta);
        }
    }

    /**
     * Comprobacion 8b: un archivo que ya esta en la version actual no se
     * reescribe al recargar (ni crea .bak). Es el defecto que rompia
     * configuraciones personalizadas: /rewards reload volvia a inyectar
     * opciones que el servidor habia borrado a proposito.
     */
    public void fusionNoReescribeAlDia() {
        if (!base.activo()) {
            registro.saltado("fusion al dia: JPRewards no esta cargado y activo");
            return;
        }
        Class<?> claseArchivos = Reflexion.clase(base.cargador(), "me.jp.jprewards.config.Archivos");
        Method actualizar = Reflexion.metodo(claseArchivos, "actualizar", String.class, int.class);
        if (claseArchivos == null || actualizar == null) {
            registro.saltado("fusion al dia: no existe me.jp.jprewards.config.Archivos.actualizar(String, int)");
            return;
        }
        File carpeta = new File(base.jprewards().getDataFolder(), "harness-merge-al-dia");
        try {
            ChequeosDatos.borrar(carpeta);
            if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
                registro.fallo("fusion al dia: no se pudo crear la carpeta temporal " + carpeta.getPath());
                return;
            }
            String texto = "config-version: 2\n\n# archivo personalizado del harness\nidioma: harness-al-dia\n";
            File destino = new File(carpeta, "config.yml");
            Files.write(destino.toPath(), texto.getBytes(StandardCharsets.UTF_8));
            Object archivos = Reflexion.nuevo(claseArchivos,
                    new Class<?>[] { File.class, Logger.class },
                    new Object[] { carpeta, log });
            Reflexion.invocar(actualizar, archivos, "config.yml", Integer.valueOf(2));

            List<String> problemas = new ArrayList<String>();
            if (!texto.equals(YamlUtil.leerTexto(destino))) {
                problemas.add("el archivo cambio aunque ya estaba al dia");
            }
            if (new File(carpeta, "config.yml.bak").isFile()) {
                problemas.add("creo un .bak innecesario");
            }
            try {
                YamlConfiguration cargado = YamlConfiguration.loadConfiguration(destino);
                if (!"harness-al-dia".equals(cargado.getString("idioma"))) {
                    problemas.add("no se lee el valor del usuario");
                }
            } catch (Throwable error) {
                problemas.add("el YAML resultante no se puede leer (" + Registro.describir(error) + ")");
            }
            if (problemas.isEmpty()) {
                registro.ok("fusion al dia: un archivo con config-version=2 no se reescribe ni crea .bak al recargar");
            } else {
                registro.fallo("fusion al dia: " + problemas);
            }
        } catch (Throwable error) {
            registro.fallo("fusion al dia: " + Registro.describir(error));
        } finally {
            ChequeosDatos.borrar(carpeta);
        }
    }

    /**
     * Comprobacion 8c: fusionar en un archivo con listas personalizadas (items
     * con ':' y con la misma sangria que su clave) no rompe el YAML ni toca
     * esas listas; las opciones nuevas de mapping si se agregan.
     */
    public void fusionRespetaListas() {
        if (!base.activo()) {
            registro.saltado("fusion con listas: JPRewards no esta cargado y activo");
            return;
        }
        Class<?> claseArchivos = Reflexion.clase(base.cargador(), "me.jp.jprewards.config.Archivos");
        Method actualizar = Reflexion.metodo(claseArchivos, "actualizar", String.class, int.class);
        if (claseArchivos == null || actualizar == null) {
            registro.saltado("fusion con listas: no existe me.jp.jprewards.config.Archivos.actualizar(String, int)");
            return;
        }
        File carpeta = new File(base.jprewards().getDataFolder(), "harness-merge-listas");
        try {
            ChequeosDatos.borrar(carpeta);
            if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
                registro.fallo("fusion con listas: no se pudo crear la carpeta temporal " + carpeta.getPath());
                return;
            }
            String texto = "config-version: 1\n"
                    + "\n"
                    + "# mensajes personalizados del harness: listas con ':' y con la misma sangria\n"
                    + "prefijo: '&7[harness] &f'\n"
                    + "\n"
                    + "comando:\n"
                    + "  ayuda-encabezado: 'cabecera: personalizada'\n"
                    + "  ayuda-lineas:\n"
                    + "  - 'uno: con dos puntos'\n"
                    + "  - 'dos: y mas'\n";
            File destino = new File(carpeta, "messages_es.yml");
            Files.write(destino.toPath(), texto.getBytes(StandardCharsets.UTF_8));
            Object archivos = Reflexion.nuevo(claseArchivos,
                    new Class<?>[] { File.class, Logger.class },
                    new Object[] { carpeta, log });
            Reflexion.invocar(actualizar, archivos, "messages_es.yml", Integer.valueOf(2));

            List<String> problemas = new ArrayList<String>();
            try {
                YamlConfiguration despues = YamlConfiguration.loadConfiguration(destino);
                if (!"cabecera: personalizada".equals(despues.getString("comando.ayuda-encabezado"))) {
                    problemas.add("se perdio el valor personalizado de comando.ayuda-encabezado");
                }
                List<String> lineas = despues.getStringList("comando.ayuda-lineas");
                List<String> esperadas = new ArrayList<String>();
                esperadas.add("uno: con dos puntos");
                esperadas.add("dos: y mas");
                if (!esperadas.equals(lineas)) {
                    problemas.add("la lista personalizada cambio: " + lineas);
                }
                if (despues.getInt("config-version") != 2) {
                    problemas.add("config-version=" + despues.getInt("config-version") + " (se esperaba 2)");
                }
                if (!despues.isSet("comando.estado-lineas")) {
                    problemas.add("no se agrego la opcion nueva comando.estado-lineas");
                }
            } catch (Throwable error) {
                problemas.add("el YAML resultante no se puede leer (" + Registro.describir(error) + ")");
            }
            List<String> duplicadas = YamlUtil.clavesDuplicadas(destino);
            if (!duplicadas.isEmpty()) {
                problemas.add("claves duplicadas: " + duplicadas);
            }
            if (!new File(carpeta, "messages_es.yml.bak").isFile()) {
                problemas.add("no se guardo la copia .bak");
            }
            if (problemas.isEmpty()) {
                registro.ok("fusion con listas: las listas personalizadas con ':' quedan intactas, "
                        + "las opciones nuevas se agregan y el YAML sigue valido");
            } else {
                registro.fallo("fusion con listas: " + problemas);
            }
        } catch (Throwable error) {
            registro.fallo("fusion con listas: " + Registro.describir(error));
        } finally {
            ChequeosDatos.borrar(carpeta);
        }
    }

    /** Primera ruta escalar del YAML (por ejemplo reinicio.hora), o null. */    private static String buscarEscalar(ConfigurationSection seccion, String prefijo, Object[] valor) {
        for (String clave : seccion.getKeys(false)) {
            String ruta = prefijo.isEmpty() ? clave : prefijo + "." + clave;
            if (seccion.isConfigurationSection(clave)) {
                String encontrada = buscarEscalar(seccion.getConfigurationSection(clave), ruta, valor);
                if (encontrada != null) {
                    return encontrada;
                }
            } else if (!"config-version".equals(ruta)) {
                valor[0] = seccion.get(clave);
                return ruta;
            }
        }
        return null;
    }

    /** Escribe la ruta anidada con sangria de 2 espacios. */
    private static void escribirRuta(StringBuilder texto, String ruta, Object valor) {
        String[] partes = ruta.split("\\.");
        for (int i = 0; i < partes.length - 1; i++) {
            texto.append(sangria(i)).append(partes[i]).append(':').append('\n');
        }
        texto.append(sangria(partes.length - 1)).append(partes[partes.length - 1])
                .append(": ").append(String.valueOf(valor)).append('\n');
    }

    private static String sangria(int nivel) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < nivel; i++) {
            texto.append("  ");
        }
        return texto.toString();
    }

    /** Valor distinto del original, del mismo tipo. */
    private static Object valorDePrueba(Object original) {
        if (original instanceof Boolean) {
            return Boolean.valueOf(!((Boolean) original).booleanValue());
        }
        if (original instanceof Integer) {
            return Integer.valueOf(4242);
        }
        if (original instanceof Long) {
            return Long.valueOf(4242L);
        }
        if (original instanceof Double) {
            return Double.valueOf(4.25D);
        }
        return "harness-valor";
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() > maximo ? texto.substring(0, maximo - 3) + "..." : texto;
    }
}
