package me.jp.jprewards.storage;

import me.jp.jprewards.model.DatosJugador;

import java.util.Set;
import java.util.UUID;

/**
 * Contrato del almacenamiento de datos. Hay dos implementaciones: YAML
 * (archivos) y MySQL (JDBC), y las dos guardan exactamente el mismo formato.
 */
public interface Almacen {

    /** Prepara el backend (crea carpetas, abre la conexion, crea la tabla). */
    void iniciar() throws Exception;

    /** Datos de un jugador (vacios si todavia no tiene). */
    DatosJugador cargar(UUID uuid, String nombre);

    /**
     * Guarda los datos YA SERIALIZADOS de un jugador.
     *
     * El plugin serializa en el hilo principal (rapido y sin compartir
     * objetos mutables) y el almacen escribe despues, incluso desde un hilo
     * aparte: asi ningun guardado puede bloquear el servidor.
     */
    void guardarTexto(java.util.UUID uuid, String nombre, String texto);

    /** Guarda los datos de un jugador (atajo que serializa y llama a guardarTexto). */
    default void guardar(DatosJugador datos) {
        guardarTexto(datos.uuid(), datos.nombre(), DatosCodec.aTexto(datos));
    }

    /** Guarda varios jugadores de una vez (autoguardado y apagado del servidor). */
    default void guardarVarios(java.util.Collection<DatosJugador> jugadores) {
        for (DatosJugador datos : jugadores) {
            guardar(datos);
        }
    }

    /** UUID de todos los jugadores con datos guardados. */
    Set<UUID> conocidos();

    /** Borra los datos de un jugador (no se usa salvo peticion del admin). */
    void eliminar(UUID uuid);

    /**
     * Marca de version de los datos de un jugador. La usa la cache para darse
     * cuenta de que otro servidor (u otra instancia) los ha cambiado. Los
     * almacenes que no la soportan devuelven 0.
     */
    default long version(java.util.UUID uuid) {
        return 0L;
    }

    /** true si el almacen puede decir si los datos cambiaron por fuera. */
    default boolean soportaVersiones() {
        return false;
    }

    /** Texto para /rewards info: donde se esta guardando (sin contrasenas). */
    String describir();

    /** Cierra el backend (al desactivar el plugin). */
    void cerrar();
}
