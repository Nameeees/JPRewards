package me.jp.jprewards.storage;

import me.jp.jprewards.model.DatosJugador;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Convierte los datos de un jugador a YAML y de vuelta.
 *
 * Se usa el MISMO formato en los tres sitios donde se guardan datos (un
 * archivo por jugador, un archivo unico y la base de datos), asi que:
 *   - migrar de un backend a otro es copiar texto, sin conversiones;
 *   - un administrador puede abrir el archivo y entenderlo;
 *   - agregar campos nuevos no rompe los datos antiguos (los que falten usan
 *     los valores por defecto).
 */
public final class DatosCodec {

    private DatosCodec() {
    }

    /** Datos -> texto YAML. */
    public static String aTexto(DatosJugador datos) {
        return aYaml(datos).saveToString();
    }

    /** Texto YAML -> datos (si algo falla devuelve unos datos vacios). */
    public static DatosJugador desdeTexto(UUID uuid, String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return new DatosJugador(uuid);
        }
        YamlConfiguration yml;
        try {
            yml = YamlConfiguration.loadConfiguration(new StringReader(texto));
        } catch (Throwable error) {
            throw new ErrorAlmacen("Los datos de " + uuid + " is not valid YAML: " + error.getMessage(), error);
        }
        return desdeYaml(uuid, yml);
    }

    /** Datos -> configuracion YAML. */
    public static YamlConfiguration aYaml(DatosJugador datos) {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("nombre", datos.nombre());
        yml.set("ultimo-dia", Long.valueOf(datos.ultimoDia()));
        yml.set("racha", Integer.valueOf(datos.racha()));
        yml.set("mejor-racha", Integer.valueOf(datos.mejorRacha()));
        yml.set("total", Integer.valueOf(datos.total()));
        yml.set("ultima-semana", Long.valueOf(datos.ultimaSemana()));
        yml.set("racha-semanal", Integer.valueOf(datos.rachaSemanal()));
        yml.set("mejor-racha-semanal", Integer.valueOf(datos.mejorRachaSemanal()));
        yml.set("total-semanal", Integer.valueOf(datos.totalSemanal()));
        yml.set("mes", Integer.valueOf(datos.mesClave()));
        yml.set("dias-mes", Integer.valueOf(datos.diasMes()));
        yml.set("hitos-reclamados", new ArrayList<String>(datos.hitosReclamados()));
        yml.set("hitos-historicos", Integer.valueOf(datos.hitosHistoricos()));
        yml.set("bonus-reclamados", new ArrayList<String>(datos.bonusReclamados()));
        yml.set("racha-bonus", Integer.valueOf(datos.rachaBonusClave()));
        yml.set("primer-reclamo", Long.valueOf(datos.primerReclamo()));
        yml.set("ultimo-reclamo", Long.valueOf(datos.ultimoReclamo()));
        if (datos.tienePendientes()) {
            yml.set("pendientes", datos.pendientes());
        }
        return yml;
    }

    /** Configuracion YAML -> datos. */
    public static DatosJugador desdeYaml(UUID uuid, YamlConfiguration yml) {
        return desdeSeccion(uuid, yml);
    }

    /** Seccion YAML -> datos (se usa tambien en el modo de archivo unico). */
    @SuppressWarnings("unchecked")
    public static DatosJugador desdeSeccion(UUID uuid, org.bukkit.configuration.ConfigurationSection yml) {
        DatosJugador datos = new DatosJugador(uuid);
        if (yml == null) {
            return datos;
        }
        Set<String> hitos = new LinkedHashSet<String>(yml.getStringList("hitos-reclamados"));
        Set<String> bonus = new LinkedHashSet<String>(yml.getStringList("bonus-reclamados"));
        datos.cargarEstado(
                yml.getString("nombre", ""),
                yml.getLong("ultimo-dia", 0L),
                yml.getInt("racha", 0),
                yml.getInt("mejor-racha", 0),
                yml.getInt("total", 0),
                yml.getLong("ultima-semana", 0L),
                yml.getInt("racha-semanal", 0),
                yml.getInt("mejor-racha-semanal", 0),
                yml.getInt("total-semanal", 0),
                yml.getInt("mes", 0),
                yml.getInt("dias-mes", 0),
                hitos,
                yml.getInt("hitos-historicos", hitos.size()),
                bonus,
                yml.getInt("racha-bonus", 0),
                yml.getLong("primer-reclamo", 0L),
                yml.getLong("ultimo-reclamo", 0L));
        datos.cargarPendientes(leerPendientes(yml));
        return datos;
    }

    /** Los items pendientes se guardan como lista de ItemStack (Bukkit los serializa). */
    @SuppressWarnings("unchecked")
    private static List<ItemStack> leerPendientes(org.bukkit.configuration.ConfigurationSection yml) {
        List<ItemStack> items = new ArrayList<ItemStack>();
        List<?> lista = yml.getList("pendientes");
        if (lista == null) {
            return items;
        }
        for (Object elemento : lista) {
            if (elemento instanceof ItemStack) {
                items.add(((ItemStack) elemento).clone());
            } else if (elemento instanceof java.util.Map) {
                try {
                    ItemStack item = ItemStack.deserialize((java.util.Map<String, Object>) elemento);
                    if (item != null) {
                        items.add(item);
                    }
                } catch (Throwable ignorado) {
                    // item ilegible: se descarta solo ese
                }
            }
        }
        return items;
    }
}
