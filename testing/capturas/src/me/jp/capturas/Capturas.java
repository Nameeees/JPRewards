package me.jp.capturas;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Plugin de ayuda SOLO para hacer capturas: no forma parte del producto.
 *
 * Cada segundo lee plugins/Capturas/orden.txt. Si la primera linea cambia,
 * la ejecuta como el jugador indicado en plugins/Capturas/jugador.txt:
 *
 *   cmd:rewards daily      -> el jugador ejecuta el comando (abre su menu)
 *   consola:op Captura     -> comando de consola
 *   chat:Hola              -> mensaje al jugador
 *
 * Asi se puede abrir cada menu desde fuera, sin teclado ni foco.
 */
public final class Capturas extends JavaPlugin {

    private String ultima = "";

    @Override
    public void onEnable() {
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                revisar();
            }
        }, 20L, 20L);
        getLogger().info("Capturas listo: escribe en " + new File(getDataFolder(), "orden.txt").getAbsolutePath());
    }

    private void revisar() {
        File archivo = new File(getDataFolder(), "orden.txt");
        if (!archivo.isFile()) {
            return;
        }
        try {
            List<String> lineas = Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
            if (lineas.isEmpty()) {
                return;
            }
            String orden = lineas.get(0).trim();
            if (orden.isEmpty() || orden.equals(ultima)) {
                return;
            }
            ultima = orden;
            ejecutar(orden);
        } catch (Throwable error) {
            getLogger().warning("orden fallida: " + error.getMessage());
        }
    }

    private void ejecutar(String orden) {
        if (orden.startsWith("consola:")) {
            String comando = orden.substring("consola:".length()).trim();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);
            getLogger().info("CONSOLA -> " + comando);
            return;
        }
        String nombre = "Captura";
        File archivo = new File(getDataFolder(), "jugador.txt");
        if (archivo.isFile()) {
            try {
                String contenido = new String(Files.readAllBytes(archivo.toPath()), StandardCharsets.UTF_8).trim();
                if (!contenido.isEmpty()) {
                    nombre = contenido;
                }
            } catch (Throwable ignorado) {
                // se usa el nombre por defecto
            }
        }
        Player jugador = Bukkit.getPlayerExact(nombre);
        if (jugador == null) {
            getLogger().warning("el jugador " + nombre + " no esta conectado");
            return;
        }
        if (orden.startsWith("cmd:")) {
            String comando = orden.substring("cmd:".length()).trim();
            jugador.performCommand(comando);
            getLogger().info("JUGADOR " + nombre + " -> /" + comando);
        } else if (orden.startsWith("chat:")) {
            jugador.sendMessage(orden.substring("chat:".length()).trim());
        }
    }
}
