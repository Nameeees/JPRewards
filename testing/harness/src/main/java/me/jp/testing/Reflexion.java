package me.jp.testing;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utilidades de reflexion.
 *
 * El harness compila sin conocer las clases de JPRewards: todas se buscan por
 * nombre. Si una clase o un metodo todavia no existen (el plugin esta a medio
 * escribir) el harness devuelve null y la comprobacion se marca SKIPPED.
 */
public final class Reflexion {

    private Reflexion() {
    }

    /** Carga una clase con el cargador del plugin bajo prueba (null si no existe). */
    public static Class<?> clase(ClassLoader cargador, String nombre) {
        if (cargador == null || nombre == null) {
            return null;
        }
        try {
            return Class.forName(nombre, false, cargador);
        } catch (Throwable ausente) {
            return null;
        }
    }

    /** Metodo publico (o declarado) con esos tipos exactos, o null. */
    public static Method metodo(Class<?> clase, String nombre, Class<?>... tipos) {
        if (clase == null || nombre == null) {
            return null;
        }
        try {
            return clase.getMethod(nombre, tipos);
        } catch (Throwable sinPublico) {
            // Se prueba con los declarados (por si el metodo no es publico).
        }
        try {
            Method declarado = clase.getDeclaredMethod(nombre, tipos);
            declarado.setAccessible(true);
            return declarado;
        } catch (Throwable ausente) {
            return null;
        }
    }

    /** Primer metodo publico con ese nombre y ese numero de parametros, o null. */
    public static Method metodoCon(Class<?> clase, String nombre, int parametros) {
        if (clase == null || nombre == null) {
            return null;
        }
        Method[] metodos;
        try {
            metodos = clase.getMethods();
        } catch (Throwable error) {
            return null;
        }
        for (Method metodo : metodos) {
            if (metodo.getName().equals(nombre) && metodo.getParameterTypes().length == parametros) {
                return metodo;
            }
        }
        return null;
    }

    /** Invoca un metodo y desenvuelve la excepcion real (sin InvocationTargetException). */
    public static Object invocar(Method metodo, Object receptor, Object... argumentos) throws Exception {
        if (metodo == null) {
            throw new NoSuchMethodException("metodo nulo");
        }
        try {
            return metodo.invoke(receptor, argumentos);
        } catch (InvocationTargetException envuelto) {
            Throwable causa = envuelto.getCause();
            if (causa instanceof Exception) {
                throw (Exception) causa;
            }
            throw new RuntimeException(causa == null ? envuelto : causa);
        }
    }

    /** Crea una instancia buscando el constructor por tipos. */
    public static Object nuevo(Class<?> clase, Class<?>[] tipos, Object[] argumentos) throws Exception {
        if (clase == null) {
            throw new NoSuchMethodException("clase nula");
        }
        Constructor<?> constructor;
        try {
            constructor = clase.getConstructor(tipos);
        } catch (NoSuchMethodException sinPublico) {
            constructor = clase.getDeclaredConstructor(tipos);
            constructor.setAccessible(true);
        }
        try {
            return constructor.newInstance(argumentos);
        } catch (InvocationTargetException envuelto) {
            Throwable causa = envuelto.getCause();
            if (causa instanceof Exception) {
                throw (Exception) causa;
            }
            throw new RuntimeException(causa == null ? envuelto : causa);
        }
    }

    /** Lee un campo (aunque sea privado), o null si no existe. */
    public static Object campo(Object objeto, String nombre) {
        if (objeto == null || nombre == null) {
            return null;
        }
        for (Class<?> actual = objeto.getClass(); actual != null; actual = actual.getSuperclass()) {
            try {
                Field campo = actual.getDeclaredField(nombre);
                campo.setAccessible(true);
                return campo.get(objeto);
            } catch (Throwable ignorado) {
                // Se sigue subiendo por la jerarquia.
            }
        }
        return null;
    }

    /** Tipos de una lista de valores (para elegir el constructor). */
    public static Class<?>[] tiposDe(Object... valores) {
        Class<?>[] tipos = new Class<?>[valores.length];
        for (int i = 0; i < valores.length; i++) {
            tipos[i] = valores[i] == null ? Object.class : valores[i].getClass();
        }
        return tipos;
    }
}
