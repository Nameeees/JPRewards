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

    /** Primera ruta escalar del YAML (por ejemplo reinicio.hora), o null. */
    private static String buscarEscalar(ConfigurationSection seccion, String prefijo, Object[] valor) {
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
