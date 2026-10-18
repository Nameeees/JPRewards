package me.jp.testing;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Comprobaciones 6 y 7: ida y vuelta de los datos.
 *
 *   6. DatosCodec: DatosJugador -> texto -> DatosJugador (con un jugador de
 *      valores conocidos: racha 12, total 40, diasMes 9, un hito, un bonus y
 *      dos items pendientes).
 *   7. AlmacenYaml: guardar y volver a cargar el mismo jugador con un almacen
 *      real en una carpeta temporal.
 *
 * Si la API todavia no esta completa (faltan clases o metodos) se marca
 * SKIPPED en vez de fallar.
 */
public final class ChequeosDatos {

    private static final UUID UUID_PRUEBA = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String NOMBRE = Chequeos.JUGADOR;

    private final Chequeos base;
    private final Registro registro;

    public ChequeosDatos(Chequeos base, Registro registro) {
        this.base = base;
        this.registro = registro;
    }

    // ------------------------------------------------------------------
    // 6. DatosCodec
    // ------------------------------------------------------------------

    public void codecIdaYVuelta() {
        if (!base.activo()) {
            registro.saltado("DatosCodec: JPRewards no esta cargado y activo");
            return;
        }
        Class<?> claseDatos = Reflexion.clase(base.cargador(), "me.jp.jprewards.model.DatosJugador");
        Class<?> claseCodec = Reflexion.clase(base.cargador(), "me.jp.jprewards.storage.DatosCodec");
        if (claseDatos == null) {
            registro.saltado("DatosCodec: no existe aun la clase me.jp.jprewards.model.DatosJugador");
            return;
        }
        if (claseCodec == null) {
            registro.saltado("DatosCodec: no existe aun la clase me.jp.jprewards.storage.DatosCodec");
            return;
        }
        Method aTexto = Reflexion.metodo(claseCodec, "aTexto", claseDatos);
        if (aTexto == null) {
            aTexto = Reflexion.metodoCon(claseCodec, "aTexto", 1);
        }
        Method desdeTexto = Reflexion.metodo(claseCodec, "desdeTexto", UUID.class, String.class);
        if (desdeTexto == null) {
            desdeTexto = Reflexion.metodoCon(claseCodec, "desdeTexto", 2);
        }
        if (aTexto == null || desdeTexto == null) {
            registro.saltado("DatosCodec: faltan los metodos aTexto(DatosJugador)/desdeTexto(UUID, String)");
            return;
        }
        try {
            List<String> faltan = new ArrayList<String>();
            Object original = datosConocidos(claseDatos, faltan);
            if (!faltan.isEmpty()) {
                registro.saltado("DatosCodec: la API de DatosJugador aun no esta completa (faltan " + faltan + ")");
                return;
            }
            Object texto = Reflexion.invocar(aTexto, null, original);
            if (!(texto instanceof String) || ((String) texto).trim().isEmpty()) {
                registro.fallo("DatosCodec.aTexto devolvio un texto vacio o de tipo " + (texto == null ? "null" : texto.getClass().getName()));
                return;
            }
            registro.info("[harness] DatosCodec.aTexto -> " + unaLinea((String) texto, 400));
            Object copia = Reflexion.invocar(desdeTexto, null, UUID_PRUEBA, texto);
            if (copia == null) {
                registro.fallo("DatosCodec.desdeTexto devolvio null");
                return;
            }
            List<String> diferencias = Comparador.diferencias(claseDatos, original, copia);
            if (diferencias.isEmpty()) {
                registro.ok("DatosCodec: ida y vuelta correcta con racha 12, total 40, diasMes 9, 1 hito, 1 bonus y 2 items pendientes");
            } else {
                registro.fallo("DatosCodec: campos distintos tras la ida y vuelta -> " + diferencias);
            }
        } catch (Throwable error) {
            registro.fallo("DatosCodec: " + Registro.describir(error));
        }
    }

    // ------------------------------------------------------------------
    // 7. AlmacenYaml
    // ------------------------------------------------------------------

    public void almacenYamlIdaYVuelta() {
        if (!base.activo()) {
            registro.saltado("AlmacenYaml: JPRewards no esta cargado y activo");
            return;
        }
        Class<?> claseDatos = Reflexion.clase(base.cargador(), "me.jp.jprewards.model.DatosJugador");
        Class<?> claseAlmacen = Reflexion.clase(base.cargador(), "me.jp.jprewards.storage.AlmacenYaml");
        Class<?> clasePlugin = Reflexion.clase(base.cargador(), "me.jp.jprewards.JPRewards");
        if (claseDatos == null) {
            registro.saltado("AlmacenYaml: no existe aun la clase me.jp.jprewards.model.DatosJugador");
            return;
        }
        if (claseAlmacen == null) {
            registro.saltado("AlmacenYaml: no existe aun la clase me.jp.jprewards.storage.AlmacenYaml");
            return;
        }
        if (clasePlugin == null || !clasePlugin.isInstance(base.jprewards())) {
            registro.saltado("AlmacenYaml: el plugin cargado no es la version nueva (me.jp.jprewards.JPRewards)");
            return;
        }
        File carpeta = new File(base.jprewards().getDataFolder(), "harness-tmp");
        try {
            borrar(carpeta);
            List<String> faltan = new ArrayList<String>();
            Object original = datosConocidos(claseDatos, faltan);
            if (!faltan.isEmpty()) {
                registro.saltado("AlmacenYaml: la API de DatosJugador aun no esta completa (faltan " + faltan + ")");
                return;
            }
            Method iniciar = Reflexion.metodo(claseAlmacen, "iniciar");
            Method guardar = Reflexion.metodoCon(claseAlmacen, "guardar", 1);
            Method cargar = Reflexion.metodo(claseAlmacen, "cargar", UUID.class, String.class);
            Method cerrar = Reflexion.metodo(claseAlmacen, "cerrar");
            if (iniciar == null || guardar == null || cargar == null) {
                registro.saltado("AlmacenYaml: faltan los metodos iniciar/guardar/cargar");
                return;
            }
            Object almacen = Reflexion.nuevo(claseAlmacen,
                    new Class<?>[] { clasePlugin, String.class, boolean.class },
                    new Object[] { base.jprewards(), "harness-tmp", Boolean.FALSE });
            Reflexion.invocar(iniciar, almacen);
            File archivo = new File(carpeta, UUID_PRUEBA.toString() + ".yml");
            Reflexion.invocar(guardar, almacen, original);
            boolean escrito = archivo.isFile() && archivo.length() > 0L;
            Object leidos = Reflexion.invocar(cargar, almacen, UUID_PRUEBA, NOMBRE);
            List<String> diferencias = Comparador.diferencias(claseDatos, original, leidos);
            if (cerrar != null) {
                Reflexion.invocar(cerrar, almacen);
            }
            if (!escrito) {
                registro.fallo("AlmacenYaml: no se creo el archivo " + archivo.getName() + " en " + carpeta.getPath());
                return;
            }
            if (diferencias.isEmpty()) {
                registro.ok("AlmacenYaml: guardar y volver a cargar devuelve los mismos datos (archivo " + archivo.getName() + ", " + archivo.length() + " bytes)");
            } else {
                registro.fallo("AlmacenYaml: los datos cargados no coinciden -> " + diferencias);
            }
        } catch (Throwable error) {
            registro.fallo("AlmacenYaml: " + Registro.describir(error));
        } finally {
            borrar(carpeta);
        }
    }

    // ------------------------------------------------------------------

    /**
     * Crea un DatosJugador con valores conocidos.
     *
     * mesClave no tiene setter: se fija con asegurarMes, que ademas reinicia
     * dias e hitos, asi que se llama ANTES de poner diasMes y los hitos.
     */
    private Object datosConocidos(Class<?> claseDatos, List<String> faltan) throws Exception {
        Object datos = Reflexion.nuevo(claseDatos, new Class<?>[] { UUID.class }, new Object[] { UUID_PRUEBA });
        if (!poner(claseDatos, datos, "asegurarMes", Integer.valueOf(202610))) {
            faltan.add("DatosJugador.asegurarMes(int)");
        }
        if (!poner(claseDatos, datos, "nombre", NOMBRE)) {
            faltan.add("DatosJugador.nombre(String)");
        }
        if (!poner(claseDatos, datos, "ultimoDia", Long.valueOf(20000L))) {
            faltan.add("DatosJugador.ultimoDia(long)");
        }
        if (!poner(claseDatos, datos, "racha", Integer.valueOf(12))) {
            faltan.add("DatosJugador.racha(int)");
        }
        if (!poner(claseDatos, datos, "mejorRacha", Integer.valueOf(15))) {
            faltan.add("DatosJugador.mejorRacha(int)");
        }
        if (!poner(claseDatos, datos, "total", Integer.valueOf(40))) {
            faltan.add("DatosJugador.total(int)");
        }
        if (!poner(claseDatos, datos, "ultimaSemana", Long.valueOf(2900L))) {
            faltan.add("DatosJugador.ultimaSemana(long)");
        }
        if (!poner(claseDatos, datos, "rachaSemanal", Integer.valueOf(3))) {
            faltan.add("DatosJugador.rachaSemanal(int)");
        }
        if (!poner(claseDatos, datos, "mejorRachaSemanal", Integer.valueOf(4))) {
            faltan.add("DatosJugador.mejorRachaSemanal(int)");
        }
        if (!poner(claseDatos, datos, "totalSemanal", Integer.valueOf(11))) {
            faltan.add("DatosJugador.totalSemanal(int)");
        }
        if (!poner(claseDatos, datos, "diasMes", Integer.valueOf(9))) {
            faltan.add("DatosJugador.diasMes(int)");
        }
        if (!poner(claseDatos, datos, "hitosHistoricos", Integer.valueOf(5))) {
            faltan.add("DatosJugador.hitosHistoricos(int)");
        }
        if (!poner(claseDatos, datos, "rachaBonusClave", Integer.valueOf(1))) {
            faltan.add("DatosJugador.rachaBonusClave(int)");
        }
        if (!poner(claseDatos, datos, "marcarReclamo", Long.valueOf(1700000000000L))) {
            faltan.add("DatosJugador.marcarReclamo(long)");
        }
        if (!agregar(claseDatos, datos, "hitosReclamados", "mes-9")) {
            faltan.add("DatosJugador.hitosReclamados()");
        }
        if (!agregar(claseDatos, datos, "bonusReclamados", "racha-7")) {
            faltan.add("DatosJugador.bonusReclamados()");
        }
        if (!pendiente(claseDatos, datos, new ItemStack(Material.DIAMOND, 3))) {
            faltan.add("DatosJugador.agregarPendiente(ItemStack)");
        }
        if (!pendiente(claseDatos, datos, new ItemStack(Material.GOLD_INGOT, 5))) {
            faltan.add("DatosJugador.agregarPendiente(ItemStack)");
        }
        return datos;
    }

    /** Llama a un metodo de un parametro con ese nombre. */
    private static boolean poner(Class<?> clase, Object datos, String nombre, Object valor) {
        Method metodo = Reflexion.metodoCon(clase, nombre, 1);
        if (metodo == null) {
            return false;
        }
        try {
            Reflexion.invocar(metodo, datos, valor);
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

    /** Anade un valor a la coleccion que devuelve ese metodo de lectura. */
    @SuppressWarnings("unchecked")
    private static boolean agregar(Class<?> clase, Object datos, String nombre, String valor) {
        Method metodo = Reflexion.metodoCon(clase, nombre, 0);
        if (metodo == null) {
            return false;
        }
        try {
            Object coleccion = Reflexion.invocar(metodo, datos);
            if (!(coleccion instanceof Set)) {
                return false;
            }
            ((Set<String>) coleccion).add(valor);
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

    /** Agrega un item pendiente. */
    private static boolean pendiente(Class<?> clase, Object datos, ItemStack item) {
        Method metodo = Reflexion.metodoCon(clase, "agregarPendiente", 1);
        if (metodo == null) {
            return false;
        }
        try {
            Reflexion.invocar(metodo, datos, item);
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

    /** Borra una carpeta con todo su contenido. */
    public static void borrar(File destino) {
        if (destino == null || !destino.exists()) {
            return;
        }
        File[] hijos = destino.listFiles();
        if (hijos != null) {
            for (File hijo : hijos) {
                borrar(hijo);
            }
        }
        destino.delete();
    }

    private static String unaLinea(String texto, int maximo) {
        String plano = texto.replace("\r", " ").replace("\n", " | ");
        if (plano.length() > maximo) {
            plano = plano.substring(0, maximo - 3) + "...";
        }
        return plano;
    }
}
