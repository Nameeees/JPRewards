package me.jp.jprewards.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Efectos y mensajes que se aplican al reclamar una recompensa.
 * Todo es opcional: lo que no se defina usa lo global de config.yml.
 *
 *   efectos:
 *     sonido: ENTITY_PLAYER_LEVELUP
 *     volumen: 0.8        # 0.0 - 1.0 (opcional)
 *     tono: 1.2           # 0.5 - 2.0 (opcional)
 *     particula: HAPPY_VILLAGER
 *     particulas: 12
 *     fuegos: true
 *     fuegos-colores: [AQUA, YELLOW]
 *     fuegos-potencia: 1
 *     titulo: true        # muestra el titulo de pantalla
 *     accion-bar: '&aHas recibido {premio}'
 *     broadcast: '&d{jugador} &7ha reclamado {premio}'   # mensaje a todo el servidor
 *   mensajes:
 *     - '&7Has recibido: &f{premio}'
 */
public final class EfectosPremio {

    private final String sonido;
    private final Float volumen;
    private final Float tono;
    private final String particula;
    private final int particulas;
    private final boolean fuegos;
    private final List<String> fuegosColores;
    private final int fuegosPotencia;
    private final Boolean titulo;
    private final String accionBar;
    private final String broadcast;
    private final List<String> mensajes;

    private EfectosPremio(String sonido, Float volumen, Float tono, String particula, int particulas, boolean fuegos,
            List<String> fuegosColores, int fuegosPotencia, Boolean titulo, String accionBar, String broadcast,
            List<String> mensajes) {
        this.sonido = sonido;
        this.volumen = volumen;
        this.tono = tono;
        this.particula = particula;
        this.particulas = particulas;
        this.fuegos = fuegos;
        this.fuegosColores = fuegosColores;
        this.fuegosPotencia = fuegosPotencia;
        this.titulo = titulo;
        this.accionBar = accionBar;
        this.broadcast = broadcast;
        this.mensajes = mensajes;
    }

    /** Efectos vacios (usa todo lo global). */
    public static EfectosPremio vacio() {
        return new EfectosPremio(null, null, null, null, 0, false, new ArrayList<String>(), 1, null, null, null,
                new ArrayList<String>());
    }

    public static EfectosPremio leer(ConfigurationSection seccion) {
        if (seccion == null) {
            return vacio();
        }
        ConfigurationSection efectos = seccion.getConfigurationSection("efectos");
        List<String> mensajes = seccion.getStringList("mensajes");
        if (efectos == null) {
            return new EfectosPremio(null, null, null, null, 0, false, new ArrayList<String>(), 1, null, null, null, mensajes);
        }
        Float volumen = efectos.isSet("volumen") ? (float) efectos.getDouble("volumen") : null;
        Float tono = efectos.isSet("tono") ? (float) efectos.getDouble("tono") : null;
        Boolean titulo = efectos.isSet("titulo") ? Boolean.valueOf(efectos.getBoolean("titulo")) : null;
        return new EfectosPremio(
                texto(efectos.getString("sonido", null)),
                volumen,
                tono,
                texto(efectos.getString("particula", null)),
                Math.max(0, efectos.getInt("particulas", 12)),
                efectos.getBoolean("fuegos", false),
                efectos.getStringList("fuegos-colores"),
                Math.max(0, efectos.getInt("fuegos-potencia", 1)),
                titulo,
                texto(efectos.getString("accion-bar", null)),
                texto(efectos.getString("broadcast", null)),
                mensajes);
    }

    private static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    /** true si no define nada (se puede reutilizar el objeto vacio). */
    public boolean estaVacio() {
        return sonido == null && particula == null && !fuegos && titulo == null && accionBar == null && broadcast == null
                && (mensajes == null || mensajes.isEmpty());
    }

    public String sonido() {
        return sonido;
    }

    public Float volumen() {
        return volumen;
    }

    public Float tono() {
        return tono;
    }

    public String particula() {
        return particula;
    }

    public int particulas() {
        return particulas;
    }

    public boolean fuegos() {
        return fuegos;
    }

    public List<String> fuegosColores() {
        return new ArrayList<String>(fuegosColores);
    }

    public int fuegosPotencia() {
        return fuegosPotencia;
    }

    /** null = usar la configuracion global. */
    public Boolean titulo() {
        return titulo;
    }

    public String accionBar() {
        return accionBar;
    }

    public String broadcast() {
        return broadcast;
    }

    public List<String> mensajes() {
        return new ArrayList<String>(mensajes);
    }
}
