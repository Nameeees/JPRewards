package me.jp.jprewards.storage;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.DatosJugador;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Almacenamiento en YAML, con dos modos (config.yml -> almacenamiento.modo):
 *
 *   por-jugador : un archivo <carpeta>/<uuid>.yml por jugador (por defecto).
 *                 Es el mas comodo: se puede revisar o editar uno solo.
 *   unico       : todo en <carpeta>/datos.yml. Menos archivos para servidores
 *                 con muchisimos jugadores.
 *
 * Escritura segura: se escribe un .tmp y se renombra encima (atomico cuando
 * el sistema lo permite), asi un corte de luz a mitad de guardado no deja un
 * archivo a medias. Si un archivo esta corrupto se guarda una copia .bak y se
 * empieza de cero en vez de sobrescribir con datos vacios sin avisar.
 */
public final class AlmacenYaml implements Almacen {

    private static final Pattern UUID_ARCHIVO = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.yml$");

    private final JPRewards plugin;
    private final File carpeta;
    private final boolean unico;
    private final File archivoUnico;

    public AlmacenYaml(JPRewards plugin, String carpeta, boolean unico) {
        this.plugin = plugin;
        this.carpeta = new File(plugin.getDataFolder(), carpeta);
        this.unico = unico;
        this.archivoUnico = new File(this.carpeta, "datos.yml");
    }

    @Override
    public void iniciar() {
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            throw new ErrorAlmacen("Could not create the data folder: " + carpeta.getPath());
        }
    }

    @Override
    public DatosJugador cargar(UUID uuid, String nombre) {
        if (unico) {
            DatosJugador datos = new DatosJugador(uuid);
            if (archivoUnico.isFile()) {
                YamlConfiguration todos = leer(archivoUnico, uuid);
                datos = DatosCodec.desdeSeccion(uuid, todos.getConfigurationSection(uuid.toString()));
            }
            if (nombre != null && !nombre.isEmpty()) {
                datos.nombre(nombre);
            }
            return datos;
        }
        File archivo = archivoDe(uuid);
        if (!archivo.isFile()) {
            DatosJugador datos = new DatosJugador(uuid);
            datos.nombre(nombre);
            return datos;
        }
        YamlConfiguration yml = leer(archivo, uuid);
        DatosJugador datos = DatosCodec.desdeYaml(uuid, yml);
        if (nombre != null && !nombre.isEmpty()) {
            datos.nombre(nombre);
        }
        return datos;
    }

    @Override
    public void guardarTexto(UUID uuid, String nombre, String texto) {
        try {
            if (unico) {
                YamlConfiguration todos = archivoUnico.isFile()
                        ? YamlConfiguration.loadConfiguration(archivoUnico)
                        : new YamlConfiguration();
                todos.set(uuid.toString(), YamlConfiguration.loadConfiguration(new java.io.StringReader(texto)));
                escribir(archivoUnico, todos.saveToString());
            } else {
                escribir(archivoDe(uuid), texto);
            }
        } catch (IOException error) {
            throw new ErrorAlmacen("Could not save the data of " + uuid + ": " + error.getMessage(), error);
        }
    }

    @Override
    public void guardarVarios(java.util.Collection<DatosJugador> jugadores) {
        if (!unico) {
            for (DatosJugador datos : jugadores) {
                guardar(datos);
            }
            return;
        }
        try {
            YamlConfiguration todos = archivoUnico.isFile()
                    ? YamlConfiguration.loadConfiguration(archivoUnico)
                    : new YamlConfiguration();
            for (DatosJugador datos : jugadores) {
                todos.set(datos.uuid().toString(), DatosCodec.aYaml(datos));
            }
            escribir(archivoUnico, todos.saveToString());
        } catch (IOException error) {
            throw new ErrorAlmacen("Could not save los datos en lote: " + error.getMessage(), error);
        }
    }

    @Override
    public Set<UUID> conocidos() {
        Set<UUID> jugadores = new LinkedHashSet<UUID>();
        if (unico) {
            if (!archivoUnico.isFile()) {
                return jugadores;
            }
            YamlConfiguration todos = YamlConfiguration.loadConfiguration(archivoUnico);
            for (String clave : todos.getKeys(false)) {
                try {
                    jugadores.add(UUID.fromString(clave));
                } catch (IllegalArgumentException ignorado) {
                    // clave que no es un uuid: se ignora
                }
            }
            return jugadores;
        }
        File[] archivos = carpeta.listFiles();
        if (archivos == null) {
            return jugadores;
        }
        for (File archivo : archivos) {
            String nombreArchivo = archivo.getName();
            if (!UUID_ARCHIVO.matcher(nombreArchivo).matches()) {
                continue;
            }
            try {
                jugadores.add(UUID.fromString(nombreArchivo.substring(0, nombreArchivo.length() - 4)));
            } catch (IllegalArgumentException ignorado) {
                // nombre raro: se ignora
            }
        }
        return jugadores;
    }

    @Override
    public void eliminar(UUID uuid) {
        if (unico) {
            if (!archivoUnico.isFile()) {
                return;
            }
            YamlConfiguration todos = YamlConfiguration.loadConfiguration(archivoUnico);
            todos.set(uuid.toString(), null);
            try {
                escribir(archivoUnico, todos.saveToString());
            } catch (IOException error) {
                throw new ErrorAlmacen("No se pudieron borrar los datos de " + uuid + ": " + error.getMessage(), error);
            }
            return;
        }
        File archivo = archivoDe(uuid);
        if (archivo.isFile() && !archivo.delete()) {
            throw new ErrorAlmacen("No se pudo borrar " + archivo.getName());
        }
    }

    @Override
    public String describir() {
        return "YAML (" + (unico ? "single file" : "one file per player") + ") in " + rutaLegible();
    }

    /** Ruta relativa al servidor (plugins/JPRewards/datos), mas corta y portable. */
    private String rutaLegible() {
        try {
            java.nio.file.Path raiz = plugin.getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
            return raiz.relativize(carpeta.toPath().toAbsolutePath().normalize()).toString().replace('\\', '/');
        } catch (Throwable error) {
            return carpeta.getPath();
        }
    }

    @Override
    public void cerrar() {
        // Nada que cerrar: los archivos se escriben en cada guardado.
    }

    // ------------------------------------------------------------------

    private File archivoDe(UUID uuid) {
        return unico ? archivoUnico : new File(carpeta, uuid.toString() + ".yml");
    }

    /**
     * Lee un archivo. Si esta corrupto lo aparta como .bak y avisa: es mejor
     * empezar de cero avisando que sobrescribir datos que el usuario si tenia.
     */
    private YamlConfiguration leer(File archivo, UUID uuid) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(archivo);
        if (yml.getKeys(false).isEmpty() && archivo.length() > 0L) {
            File copia = new File(archivo.getParentFile(), archivo.getName() + ".bak");
            try {
                Files.copy(archivo.toPath(), copia.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().severe("The data file " + archivo.getName()
                        + " could not be read (invalid YAML). A copy was saved as " + copia.getName()
                        + " and a fresh profile was started for that player.");
            } catch (IOException error) {
                plugin.getLogger().severe("The data file " + archivo.getName() + " could not be read or copied: "
                        + error.getMessage());
            }
            return new YamlConfiguration();
        }
        return yml;
    }

    /** Escritura atomica: .tmp + rename. */
    private void escribir(File destino, String texto) throws IOException {
        File padre = destino.getParentFile();
        if (padre != null && !padre.isDirectory() && !padre.mkdirs()) {
            throw new IOException("Could not create the folder " + padre.getPath());
        }
        Path temporal = new File(padre, destino.getName() + ".tmp").toPath();
        Files.write(temporal, texto.getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(temporal, destino.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException sinAtomico) {
            Files.move(temporal, destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
