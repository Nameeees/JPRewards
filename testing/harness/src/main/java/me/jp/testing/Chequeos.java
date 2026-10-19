package me.jp.testing;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Comprobaciones 1 a 5 y 10 del harness.
 *
 * Todo se hace por reflexion/Bukkit, sin conocer las clases de JPRewards: si
 * el plugin esta a medio escribir las comprobaciones que dependen de lo que
 * falta se marcan SKIPPED en vez de romper el arranque.
 */
public final class Chequeos {

    /** Archivos que JPRewards debe crear en su carpeta al arrancar (ingles + presets en espanol). */
    public static final String[] ARCHIVOS = {
            "config.yml",
            "messages_en.yml",
            "messages_es.yml",
            "menus.yml",
            "menus_es.yml",
            "recompensas/diarias.yml",
            "recompensas/diarias_es.yml",
            "recompensas/semanales.yml",
            "recompensas/semanales_es.yml",
            "recompensas/mensuales.yml",
            "recompensas/mensuales_es.yml",
            "recompensas/rachas.yml",
            "recompensas/rachas_es.yml"
    };

    /** Secciones que debe definir messages_es.yml. */
    private static final String[] SECCIONES_MENSAJES = {
            "general", "comando", "diaria", "semanal", "mensual", "rachas", "recompensa", "titulos"
    };

    /** Nombre del jugador de pruebas (existe en el usercache de los servidores). */
    public static final String JUGADOR = "HarnessPlayer";

    private final HarnessPlugin harness;
    private final Registro registro;
    private final CapturaLog captura;

    private Plugin jpr;
    private ClassLoader cargador;

    public Chequeos(HarnessPlugin harness, Registro registro, CapturaLog captura) {
        this.harness = harness;
        this.registro = registro;
        this.captura = captura;
    }

    public Plugin jprewards() {
        return jpr;
    }

    public ClassLoader cargador() {
        return cargador;
    }

    public boolean activo() {
        return jpr != null && jpr.isEnabled();
    }

    /** Salta la comprobacion si JPRewards no esta cargado (con su motivo). */
    private boolean disponible(String que) {
        if (activo()) {
            return true;
        }
        registro.saltado(que + ": JPRewards no esta cargado y activo");
        return false;
    }

    // ------------------------------------------------------------------
    // 1. Plugin activo y comandos
    // ------------------------------------------------------------------

    public void pluginYComandos() {
        Plugin encontrado = Bukkit.getPluginManager().getPlugin("JPRewards");
        if (encontrado == null) {
            if (hayJarDelPlugin()) {
                registro.fallo("JPRewards esta en plugins/ pero NO se cargo (clase principal inexistente o error en onEnable: revisa el log)");
            } else {
                registro.fallo("JPRewards no esta instalado en plugins/ (copia su jar antes de lanzar el harness)");
            }
            return;
        }
        jpr = encontrado;
        cargador = encontrado.getClass().getClassLoader();
        if (!encontrado.isEnabled()) {
            registro.fallo("JPRewards esta cargado pero NO habilitado (version " + encontrado.getDescription().getVersion() + ")");
            return;
        }
        registro.ok("JPRewards activado (version " + encontrado.getDescription().getVersion()
                + ", clase " + encontrado.getClass().getName() + ")");

        PluginCommand comando = null;
        if (encontrado instanceof JavaPlugin) {
            comando = ((JavaPlugin) encontrado).getCommand("rewards");
        }
        if (comando == null) {
            comando = Bukkit.getPluginCommand("rewards");
        }
        if (comando == null) {
            registro.fallo("el comando /rewards no esta registrado (revisa la seccion commands de plugin.yml)");
            return;
        }
        registro.ok("comando /rewards registrado (plugin " + comando.getPlugin().getName() + ")");

        boolean aliasRec = false;
        List<String> alias = comando.getAliases();
        if (alias != null) {
            for (String candidato : alias) {
                if (candidato != null && candidato.equalsIgnoreCase("rec")) {
                    aliasRec = true;
                }
            }
        }
        if (aliasRec) {
            registro.ok("el alias /rec esta registrado");
        } else {
            registro.fallo("el alias /rec NO esta registrado (plugin.yml: aliases: [..., rec, ...])");
        }

        if (comando.getExecutor() != null) {
            registro.ok("setExecutor aplicado (" + comando.getExecutor().getClass().getName() + ")");
        } else {
            registro.fallo("el comando /rewards no tiene executor (falta setExecutor)");
        }
        if (comando.getTabCompleter() != null) {
            registro.ok("setTabCompleter aplicado (" + comando.getTabCompleter().getClass().getName() + ")");
        } else {
            registro.fallo("el comando /rewards no tiene tab completer (falta setTabCompleter)");
        }
    }

    private boolean hayJarDelPlugin() {
        File carpeta = harness.getDataFolder().getParentFile();
        File[] archivos = carpeta == null ? null : carpeta.listFiles();
        if (archivos == null) {
            return false;
        }
        for (File archivo : archivos) {
            String nombre = archivo.getName().toLowerCase(Locale.ROOT);
            if (nombre.endsWith(".jar") && nombre.startsWith("jprewards") && !nombre.contains("harness")) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 2. Archivos por defecto
    // ------------------------------------------------------------------

    public void archivosPorDefecto() {
        if (!disponible("archivos por defecto")) {
            return;
        }
        File carpeta = jpr.getDataFolder();
        List<String> faltan = new ArrayList<String>();
        List<String> vacios = new ArrayList<String>();
        for (String ruta : ARCHIVOS) {
            File destino = new File(carpeta, ruta);
            if (!destino.isFile()) {
                faltan.add(ruta);
            } else if (destino.length() <= 0L) {
                vacios.add(ruta);
            }
        }
        if (faltan.isEmpty() && vacios.isEmpty()) {
            registro.ok("los " + ARCHIVOS.length + " archivos por defecto se crearon en " + carpeta.getPath()
                    + " (config.yml, messages_en/es.yml, menus.yml, menus_es.yml y los cuatro recompensas/*.yml con sus presets _es.yml)");
            return;
        }
        StringBuilder motivo = new StringBuilder();
        if (!faltan.isEmpty()) {
            motivo.append("faltan ").append(faltan);
        }
        if (!vacios.isEmpty()) {
            if (motivo.length() > 0) {
                motivo.append(" y ");
            }
            motivo.append("estan vacios ").append(vacios);
        }
        registro.fallo("archivos por defecto en " + carpeta.getPath() + ": " + motivo);
    }

    // ------------------------------------------------------------------
    // 3. YAML legibles y sin claves duplicadas
    // ------------------------------------------------------------------

    public void yamlLegibles() {
        if (!disponible("YAML legibles")) {
            return;
        }
        List<String> problemas = new ArrayList<String>();
        int leidos = 0;
        for (String ruta : ARCHIVOS) {
            File destino = new File(jpr.getDataFolder(), ruta);
            if (!destino.isFile()) {
                problemas.add(ruta + " (no existe)");
                continue;
            }
            try {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(destino);
                boolean problema = false;
                if (yml.getKeys(false).isEmpty() && destino.length() > 0L) {
                    problemas.add(ruta + " (se leyo vacio: YAML invalido)");
                    problema = true;
                }
                List<String> duplicadas = YamlUtil.clavesDuplicadas(destino);
                if (!duplicadas.isEmpty()) {
                    problemas.add(ruta + " (claves duplicadas: " + duplicadas + ")");
                    problema = true;
                }
                if (!problema) {
                    leidos++;
                }
            } catch (Throwable error) {
                problemas.add(ruta + " (" + Registro.describir(error) + ")");
            }
        }
        if (problemas.isEmpty()) {
            registro.ok("los YAML se leen con YamlConfiguration sin excepciones y sin claves duplicadas (" + leidos + " de " + ARCHIVOS.length + " archivos)");
        } else {
            registro.fallo("YAML con problemas: " + problemas);
        }
    }

    // ------------------------------------------------------------------
    // 4. config-version >= 2
    // ------------------------------------------------------------------

    public void versionesConfig() {
        if (!disponible("config-version")) {
            return;
        }
        List<String> problemas = new ArrayList<String>();
        for (String ruta : ARCHIVOS) {
            File destino = new File(jpr.getDataFolder(), ruta);
            if (!destino.isFile()) {
                problemas.add(ruta + " (no existe)");
                continue;
            }
            try {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(destino);
                if (!yml.isSet("config-version")) {
                    problemas.add(ruta + " (sin config-version)");
                    continue;
                }
                long version = yml.getLong("config-version");
                if (version < 2L) {
                    problemas.add(ruta + " (config-version=" + version + ", se espera >= 2)");
                }
            } catch (Throwable error) {
                problemas.add(ruta + " (" + Registro.describir(error) + ")");
            }
        }
        if (problemas.isEmpty()) {
            registro.ok("los " + ARCHIVOS.length + " archivos gestionados tienen config-version >= 2");
        } else {
            registro.fallo("config-version incorrecta: " + problemas);
        }
    }

    // ------------------------------------------------------------------
    // 5. Comandos desde consola
    // ------------------------------------------------------------------

    public void comandos() {
        if (!disponible("comandos de administracion")) {
            return;
        }
        String nombre = JUGADOR;
        try {
            OfflinePlayer jugador = Bukkit.getOfflinePlayer(JUGADOR);
            if (jugador != null && jugador.getName() != null && !jugador.getName().isEmpty()) {
                nombre = jugador.getName();
            }
        } catch (Throwable error) {
            registro.info("[harness] no se pudo crear el OfflinePlayer " + JUGADOR + ": " + Registro.describir(error));
        }
        registro.info("[harness] jugador de pruebas para los comandos: " + nombre);
        // Subcomandos canonicos en INGLES (son los unicos que existen) y, al
        // final, el alias heredado /recompensas de la version 1.x.
        String[] ordenes = {
                "info",
                "validate",
                "help",
                "top",
                "status",
                "data " + nombre,
                "give " + nombre + " daily",
                "reload",
                "test daily",
                "set " + nombre + " streak 3",
                "reset " + nombre + " daily"
        };
        for (String orden : ordenes) {
            ejecutarComando(orden);
        }
        // El alias heredado de la 1.x tiene que seguir funcionando.
        if (Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "recompensas status")) {
            registro.ok("el alias heredado /recompensas sigue funcionando");
        } else {
            registro.fallo("el alias heredado /recompensas no responde");
        }
        // Normalizacion de las acciones de menus.yml: los nombres nuevos y los
        // heredados de la 1.x tienen que acabar en la misma accion.
        try {
            Class<?> menuManager = Class.forName("me.jp.jprewards.menu.MenuManager", true, cargador());
            java.lang.reflect.Method normalizar = menuManager.getMethod("normalizarAccion", String.class);
            String[][] casos = {
                    {"abrir:menu-principal", "open:menu-principal"},
                    {"open:menu-principal", "open:menu-principal"},
                    {"reclamar:diaria", "claim:daily"},
                    {"claim:weekly", "claim:weekly"},
                    {"reclamar:hito:hito-5", "claim:milestone:hito-5"},
                    {"reclamar:racha:racha-7", "claim:streak:racha-7"},
                    {"cerrar", "close"},
                    {"nada", "none"},
                    {"ninguna", "none"},
                    {"bloqueado", "denied"},
                    {"comando:say hola", "command:say hola"}
            };
            int bien = 0;
            for (String[] caso : casos) {
                Object resultado = normalizar.invoke(null, caso[0]);
                if (caso[1].equals(resultado)) {
                    bien++;
                }
            }
            if (bien == casos.length) {
                registro.ok("acciones de menus.yml: " + bien + "/" + casos.length
                        + " (nombres nuevos e heredados) se normalizan a lo mismo");
            } else {
                registro.fallo("acciones de menus.yml: solo " + bien + "/" + casos.length + " se normalizan bien");
            }
        } catch (Throwable error) {
            registro.info("[harness] no se pudo comprobar el normalizador de acciones: " + Registro.describir(error));
        }
        // El reclamo diario solo se puede hacer UNA vez por dia: con los datos
        // de hoy ya marcados, la comprobacion tiene que decir que no.
        try {
            Class<?> datosClase = Class.forName("me.jp.jprewards.model.DatosJugador", true, cargador());
            Class<?> tiempoClase = Class.forName("me.jp.jprewards.util.Tiempo", true, cargador());
            Class<?> ajustesClase = Class.forName("me.jp.jprewards.config.Ajustes", true, cargador());
            Class<?> pluginClase = Class.forName("me.jp.jprewards.JPRewards", true, cargador());
            Class<?> managerClase = Class.forName("me.jp.jprewards.manager.RecompensaManager", true, cargador());

            Object datos = datosClase.getConstructor(java.util.UUID.class)
                    .newInstance(java.util.UUID.randomUUID());
            Object ajustes = pluginClase.getMethod("ajustes").invoke(jprewards());
            Object manager = pluginClase.getMethod("recompensas").invoke(jprewards());
            int hora = ((Integer) ajustesClase.getMethod("horaReinicio").invoke(ajustes)).intValue();
            long hoy = ((Long) tiempoClase.getMethod("claveDia", int.class).invoke(null, Integer.valueOf(hora)))
                    .longValue();
            java.lang.reflect.Method puede = managerClase.getMethod("puedeReclamarDiaria", datosClase);

            boolean antes = ((Boolean) puede.invoke(manager, datos)).booleanValue();
            datosClase.getMethod("ultimoDia", long.class).invoke(datos, Long.valueOf(hoy));
            boolean despues = ((Boolean) puede.invoke(manager, datos)).booleanValue();
            if (antes && !despues) {
                registro.ok("reclamo diario: un dia ya reclamado no se puede volver a reclamar (antes=si, despues=no)");
            } else {
                registro.fallo("reclamo diario: la guardia falla (antes=" + antes + ", despues=" + despues + ")");
            }
        } catch (Throwable error) {
            registro.info("[harness] no se pudo comprobar la guardia del reclamo diario: " + Registro.describir(error));
        }

        // Forma corta de recompensas: un premio por linea + packs reutilizables.
        try {
            java.io.File carpeta = new java.io.File(jprewards().getDataFolder(), "recompensas");
            carpeta.mkdirs();
            java.io.File prueba = new java.io.File(carpeta, "prueba-compacta.yml");
            String contenido = ""
                    + "config-version: 2" + "\n"
                    + "definir:" + "\n"
                    + "  vip: 'crate key vip {jugador}'" + "\n"
                    + "dias:" + "\n"
                    + "  '1': 'eco give {jugador} 100'" + "\n"
                    + "  '2': ['eco give {jugador} 200', 'BREAD 4']" + "\n"
                    + "  '3':" + "\n"
                    + "    usar: vip" + "\n"
                    + "    extra: 'give {jugador} diamond 1'" + "\n";
            java.nio.file.Files.write(prueba.toPath(),
                    contenido.getBytes(java.nio.charset.Charset.forName("UTF-8")));

            Class<?> cargadorClase = Class.forName("me.jp.jprewards.manager.Cargador", true, cargador());
            Class<?> pluginClase = Class.forName("me.jp.jprewards.JPRewards", true, cargador());
            Class<?> recompensaClase = Class.forName("me.jp.jprewards.model.Recompensa", true, cargador());
            Class<?> varianteClase = Class.forName("me.jp.jprewards.model.Recompensa$Variante", true, cargador());
            Object cargador1 = cargadorClase.getConstructor(pluginClase).newInstance(jprewards());
            java.lang.reflect.Method cargarCiclo = cargadorClase.getMethod("cargarCiclo",
                    String.class, String.class, String.class);
            java.util.Map<?, ?> ciclo = (java.util.Map<?, ?>) cargarCiclo.invoke(cargador1,
                    "recompensas/prueba-compacta.yml", "dias", "dia");

            java.lang.reflect.Method varianteBase = recompensaClase.getMethod("varianteBase");
            java.lang.reflect.Method comandos = varianteClase.getMethod("comandos");
            java.lang.reflect.Method items = varianteClase.getMethod("items");
            int comandosUno = ((java.util.List<?>) comandos.invoke(varianteBase.invoke(ciclo.get(Integer.valueOf(1))))).size();
            int comandosDos = ((java.util.List<?>) comandos.invoke(varianteBase.invoke(ciclo.get(Integer.valueOf(2))))).size();
            int itemsDos = ((java.util.List<?>) items.invoke(varianteBase.invoke(ciclo.get(Integer.valueOf(2))))).size();
            int comandosTres = ((java.util.List<?>) comandos.invoke(varianteBase.invoke(ciclo.get(Integer.valueOf(3))))).size();

            if (ciclo.size() == 3 && comandosUno == 1 && comandosDos == 1 && itemsDos == 1 && comandosTres == 2) {
                registro.ok("recompensas de una linea: comando suelto, comando+item y pack con 'extra'");
            } else {
                registro.fallo("forma corta: dias=" + ciclo.size() + " c1=" + comandosUno + " c2=" + comandosDos
                        + " i2=" + itemsDos + " c3=" + comandosTres);
            }
            prueba.delete();
        } catch (Throwable error) {
            registro.info("[harness] no se pudo comprobar la forma corta: " + Registro.describir(error));
        }

        // No debe existir ningun permiso que permita reclamar de nuevo.
        try {
            boolean existe = false;
            for (org.bukkit.permissions.Permission permiso : jprewards().getDescription().getPermissions()) {
                if ("jprewards.bypass".equals(permiso.getName())) {
                    existe = true;
                }
            }
            if (existe) {
                registro.fallo("jprewards.bypass sigue registrado: se podria reclamar la recompensa sin limite");
            } else {
                registro.ok("no hay ningun permiso que permita saltarse el bloqueo y reclamar dos veces");
            }
        } catch (Throwable error) {
            registro.info("[harness] no se pudieron leer los permisos: " + Registro.describir(error));
        }

        if (activo()) {
            registro.ok("el plugin sigue activo despues de los comandos");
        } else {
            registro.fallo("el plugin dejo de estar activo al ejecutar los comandos");
        }
    }

    private void ejecutarComando(String orden) {
        String etiqueta = "comando /rewards " + orden;
        captura.limpiar();
        captura.armar();
        boolean devuelto = false;
        Throwable lanzado = null;
        try {
            devuelto = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rewards " + orden);
        } catch (Throwable error) {
            lanzado = error;
        }
        captura.desarmar();
        List<String> sucesos = captura.sucesos();
        if (lanzado != null) {
            registro.fallo(etiqueta + " lanzo una excepcion: " + Registro.describir(lanzado));
        } else if (!sucesos.isEmpty()) {
            registro.fallo(etiqueta + " dejo un error en el log: " + sucesos.get(0));
        } else if (!devuelto) {
            registro.ok(etiqueta + " sin excepciones (el plugin devolvio false: subcomando no reconocido o uso incorrecto)");
        } else {
            registro.ok(etiqueta + " sin excepciones");
        }
    }

    // ------------------------------------------------------------------
    // 10. Claves de mensajes
    // ------------------------------------------------------------------

    public void mensajes() {
        Class<?> claseMensajes = Reflexion.clase(cargador, "me.jp.jprewards.config.Mensajes");
        if (claseMensajes == null) {
            registro.saltado("mensajes: no existe aun la clase me.jp.jprewards.config.Mensajes");
            return;
        }
        if (!disponible("mensajes")) {
            return;
        }
        File archivo = new File(jpr.getDataFolder(), "messages_es.yml");
        if (!archivo.isFile()) {
            registro.fallo("mensajes: no existe messages_es.yml en " + jpr.getDataFolder().getPath());
            return;
        }
        YamlConfiguration yml;
        try {
            yml = YamlConfiguration.loadConfiguration(archivo);
        } catch (Throwable error) {
            registro.fallo("mensajes: messages_es.yml no se puede leer (" + Registro.describir(error) + ")");
            return;
        }
        List<String> faltan = new ArrayList<String>();
        if (!yml.isSet("prefijo")) {
            faltan.add("prefijo");
        }
        for (String seccion : SECCIONES_MENSAJES) {
            if (!yml.isSet(seccion)) {
                faltan.add(seccion);
            }
        }
        if (faltan.isEmpty()) {
            registro.ok("mensajes: messages_es.yml define prefijo y las secciones general, comando, diaria, semanal, mensual, rachas, recompensa y titulos");
        } else {
            registro.fallo("mensajes: faltan claves/secciones en messages_es.yml -> " + faltan);
        }
    }
}
