package me.jp.jprewards.storage;

/**
 * Error de almacenamiento: se lanza cuando los datos no se pueden leer o
 * escribir. El plugin lo captura, avisa por consola y sigue funcionando.
 */
public class ErrorAlmacen extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ErrorAlmacen(String mensaje) {
        super(mensaje);
    }

    public ErrorAlmacen(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
