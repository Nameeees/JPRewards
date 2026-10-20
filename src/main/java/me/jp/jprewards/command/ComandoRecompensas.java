package me.jp.jprewards.command;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.config.Mensajes;
import me.jp.jprewards.config.Validador;
import me.jp.jprewards.manager.Estadisticas;
import me.jp.jprewards.model.BonusRacha;
import me.jp.jprewards.model.DatosJugador;
import me.jp.jprewards.model.Hito;
import me.jp.jprewards.model.ItemPremio;
import me.jp.jprewards.model.Recompensa;
import me.jp.jprewards.util.Tiempo;
import me.jp.jprewards.util.Texto;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Comando /rewards (aliases: rewards, jprewards, rec, recompensa).
 *
 * Los subcomandos canonicos son en INGLES; las formas en espanol siguen
 * funcionando como alias (asi los servidores que ya las usan no se rompen).
 *
 * Players:
 *   /rewards                       abre el menu principal
 *   /rewards daily|weekly|monthly|streaks|info   (diaria|semanal|mensual|rachas)
 *   /rewards claim [tipo|todo]     reclama sin abrir el menu (reclamar)
 *   /rewards top [tipo]            ranking del servidor (ranking)
 *   /rewards help                  ayuda (ayuda)
 *
 * Administration:
 *   /rewards status                estado del plugin y de la configuracion (estado)
 *   /rewards validate              revisa TODOS los archivos (validar)
 *   /rewards reload [archivo]      recarga en caliente (recargar)
 *   /rewards data <jugador>        informe completo de un jugador (datos)
 *   /rewards give <jugador> <tipo> [clave]   entrega un premio (dar)
 *   /rewards test <tipo> [clave]   simulacro: muestra que se entregaria (simular)
 *   /rewards reset <jugador|*> [tipo]        (reiniciar)
 *   /rewards set <jugador> <campo> <valor>   (fijar)
 *   /rewards migrate [confirmar]   copia los datos entre YAML y MySQL (migrar)
 */
public final class ComandoRecompensas implements CommandExecutor, TabCompleter {

    /** Tipos de recompensa que aceptan claim, give, test y reset. */
    private static final List<String> TIPOS = Arrays.asList("daily", "weekly", "monthly", "streaks", "all");
    /** Campos que acepta /rewards set. */
    private static final List<String> CAMPOS = Arrays.asList("streak", "best-streak", "total", "weekly-streak",
            "weekly-total", "month-days", "milestones");
    /** Destinos de /rewards reload. */
    private static final List<String> DESTINOS_RECARGA = Arrays.asList("all", "config", "messages", "menus",
            "rewards");
    /**
     * Subcomandos canonicos, en espanol (es el idioma del comando).
     * Las formas en ingles (reload, test, set, reset, top...) se siguen
     * aceptando como alias, asi que ningun servidor que ya los use se rompe.
     */
    private static final List<String> SUBCOMANDOS = Arrays.asList("help", "daily", "weekly", "monthly", "streaks",
            "info", "claim", "top", "status", "validate", "reload", "data", "give", "test", "reset", "set",
            "migrate", "trigger", "backup");

    private final JPRewards plugin;

    public ComandoRecompensas(JPRewards plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender emisor, Command comando, String etiqueta, String[] args) {
        try {
            return ejecutar(emisor, etiqueta, args);
        } catch (Throwable error) {
            // Con la traza completa: sin ella no hay forma de arreglar el fallo.
            plugin.getLogger().severe("Error running /" + etiqueta + ": " + error);
            java.io.StringWriter traza = new java.io.StringWriter();
            error.printStackTrace(new java.io.PrintWriter(traza));
            for (String linea : traza.toString().split("\n")) {
                plugin.getLogger().severe("  " + linea.trim());
            }
            plugin.mensajes().enviar(emisor, "general.error");
            return true;
        }
    }

    private boolean ejecutar(CommandSender emisor, String etiqueta, String[] args) {
        if (args.length == 0) {
            abrirMenu(emisor, "menu-principal");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if ("help".equals(sub) || "?".equals(sub) || "h".equals(sub)) {
            enviarAyuda(emisor, args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : null);
            return true;
        }
        if ("daily".equals(sub) || "day".equals(sub)) {
            abrirMenu(emisor, "menu-diario");
            return true;
        }
        if ("weekly".equals(sub) || "week".equals(sub)) {
            abrirMenu(emisor, "menu-semanal");
            return true;
        }
        if ("monthly".equals(sub) || "month".equals(sub)) {
            abrirMenu(emisor, "menu-mensual");
            return true;
        }
        if ("streaks".equals(sub) || "streak".equals(sub) || "bonus".equals(sub) || "bonuses".equals(sub)) {
            abrirMenu(emisor, "menu-rachas");
            return true;
        }
        if ("info".equals(sub)) {
            abrirMenu(emisor, "menu-info");
            return true;
        }
        if ("claim".equals(sub)) {
            return reclamar(emisor, args);
        }
        if ("top".equals(sub) || "leaderboard".equals(sub) || "ranking".equals(sub)) {
            return top(emisor, args);
        }
        if ("status".equals(sub)) {
            return estado(emisor);
        }
        if ("validate".equals(sub) || "check".equals(sub)) {
            return validar(emisor);
        }
        if ("reload".equals(sub)) {
            return recargar(emisor, args);
        }
        if ("data".equals(sub)) {
            return datos(emisor, args);
        }
        if ("give".equals(sub)) {
            return dar(emisor, args);
        }
        if ("test".equals(sub) || "simulate".equals(sub)) {
            return test(emisor, args);
        }
        if ("reset".equals(sub)) {
            return reset(emisor, args);
        }
        if ("set".equals(sub)) {
            return set(emisor, args);
        }
        if ("migrate".equals(sub)) {
            return migrar(emisor, args);
        }
        if ("trigger".equals(sub) || "extra".equals(sub)) {
            return disparar(emisor, args);
        }
        if ("backup".equals(sub)) {
            return copiaSeguridad(emisor);
        }

        plugin.mensajes().enviar(emisor, "comando.desconocido", Mensajes.ph("entrada", args[0]));
        enviarAyuda(emisor, null);
        return true;
    }

    // ------------------------------------------------------------------
    // Jugadores
    // ------------------------------------------------------------------

    private void abrirMenu(CommandSender emisor, String menuId) {
        if (!(emisor instanceof Player)) {
            plugin.mensajes().enviar(emisor, "general.solo-jugadores");
            return;
        }
        Player jugador = (Player) emisor;
        if (!jugador.hasPermission("jprewards.use")) {
            plugin.mensajes().enviar(jugador, "general.sin-permiso");
            return;
        }
        plugin.menuManager().abrir(jugador, menuId);
    }

    private boolean reclamar(CommandSender emisor, String[] args) {
        if (!(emisor instanceof Player)) {
            plugin.mensajes().enviar(emisor, "general.solo-jugadores");
            return true;
        }
        Player jugador = (Player) emisor;
        if (!jugador.hasPermission("jprewards.use")) {
            plugin.mensajes().enviar(jugador, "general.sin-permiso");
            return true;
        }
        if (args.length < 2) {
            plugin.mensajes().enviar(jugador, "comando.reclamar-tipos", Mensajes.ph("tipos", String.join(", ", TIPOS)));
            return true;
        }
        String tipo = args[1].toLowerCase(Locale.ROOT);
        if (!TIPOS.contains(tipo)) {
            plugin.mensajes().enviar(jugador, "comando.reclamar-tipos", Mensajes.ph("tipos", String.join(", ", TIPOS)));
            return true;
        }
        if ("all".equals(tipo)) {
            plugin.recompensas().reclamarTodo(jugador);
            return true;
        }
        if ("daily".equals(tipo)) {
            plugin.recompensas().reclamarDiaria(jugador);
            return true;
        }
        if ("weekly".equals(tipo)) {
            plugin.recompensas().reclamarSemanal(jugador);
            return true;
        }
        if ("monthly".equals(tipo)) {
            List<Hito> disponibles = plugin.recompensas().hitosDisponibles(plugin.recompensas().datos(jugador));
            if (disponibles.isEmpty()) {
                plugin.mensajes().enviar(jugador, "mensual.nada-disponible");
                plugin.sonidos().error(jugador);
                return true;
            }
            for (Hito hito : disponibles) {
                plugin.recompensas().reclamarHito(jugador, hito);
            }
            return true;
        }
        if ("streaks".equals(tipo)) {
            List<BonusRacha> disponibles = plugin.recompensas().bonusDisponibles(plugin.recompensas().datos(jugador));
            if (disponibles.isEmpty()) {
                plugin.mensajes().enviar(jugador, "rachas.nada-disponible");
                plugin.sonidos().error(jugador);
                return true;
            }
            for (BonusRacha bono : disponibles) {
                plugin.recompensas().reclamarBonus(jugador, bono);
            }
            return true;
        }
        plugin.mensajes().enviar(jugador, "comando.reclamar-tipos", Mensajes.ph("tipos", String.join(", ", TIPOS)));
        return true;
    }

    private boolean top(CommandSender emisor, String[] args) {
        if (!emisor.hasPermission("jprewards.top")) {
            plugin.mensajes().enviar(emisor, "general.sin-permiso");
            return true;
        }
        String tipo = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "streak";
        if (!Estadisticas.tipoValido(tipo)) {
            plugin.mensajes().enviar(emisor, "comando.top-tipos", Mensajes.ph("tipos", Estadisticas.tipos()));
            return true;
        }
        final CommandSender destino = emisor;
        final String tipoFinal = tipo;
        plugin.mensajes().enviar(emisor, "comando.top-calculando");
        plugin.estadisticas().calcular(tipo, plugin.ajustes().topCantidad(), new Estadisticas.Aviso() {
            @Override
            public void listo(List<Estadisticas.Entrada> entradas) {
                Map<String, String> ph = new LinkedHashMap<String, String>();
                ph.put("tipo", tipoFinal);
                ph.put("cantidad", String.valueOf(entradas.size()));
                plugin.mensajes().enviar(destino, "comando.top-titulo", ph);
                if (entradas.isEmpty()) {
                    plugin.mensajes().enviar(destino, "comando.top-vacio");
                    return;
                }
                int puesto = 1;
                for (Estadisticas.Entrada entrada : entradas) {
                    Map<String, String> linea = new LinkedHashMap<String, String>();
                    linea.put("puesto", String.valueOf(puesto));
                    linea.put("jugador", entrada.nombre());
                    linea.put("valor", String.valueOf(entrada.valor()));
                    linea.put("tipo", tipoFinal);
                    plugin.mensajes().enviar(destino, "comando.top-linea", linea);
                    puesto++;
                }
            }
        });
        return true;
    }

    // ------------------------------------------------------------------
    // Administracion
    // ------------------------------------------------------------------

    private boolean estado(CommandSender emisor) {
        if (!esAdmin(emisor, "jprewards.admin")) {
            return true;
        }
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("version", plugin.getDescription().getVersion());
        ph.put("servidor", me.jp.jprewards.util.Compat.versionServidor());
        ph.put("idioma", plugin.mensajes().idioma());
        ph.put("almacenamiento", plugin.datos().describir());
        ph.put("tipo_almacenamiento", plugin.datos().tipoReal().clave());
        ph.put("cargados", String.valueOf(plugin.datos().enMemoria()));
        ph.put("dias", String.valueOf(plugin.recompensas().diasDelCiclo()));
        ph.put("semanas", String.valueOf(plugin.recompensas().semanasDelCiclo()));
        ph.put("hitos", String.valueOf(plugin.recompensas().hitos().size()));
        ph.put("bonus", String.valueOf(plugin.recompensas().bonus().size()));
        ph.put("autoguardado", String.valueOf(plugin.ajustes().autoguardadoSegundos()));
        ph.put("problemas", String.valueOf(plugin.ajustes().problemas().size()));
        ph.put("ruta", rutaRelativa());
        plugin.mensajes().enviar(emisor, "comando.estado-titulo", ph);
        plugin.mensajes().enviarLineas(emisor, "comando.estado-lineas", ph);
        if (plugin.hookActualizaciones() != null && plugin.hookActualizaciones().hayNueva()) {
            plugin.mensajes().enviar(emisor, "comando.estado-actualizacion",
                    Mensajes.ph("version", plugin.hookActualizaciones().ultimaVersion()));
        }
        if (!plugin.ajustes().problemas().isEmpty()) {
            plugin.mensajes().enviar(emisor, "comando.estado-problemas",
                    Mensajes.ph("cantidad", String.valueOf(plugin.ajustes().problemas().size())));
        }
        return true;
    }

    private boolean validar(CommandSender emisor) {
        if (!esAdmin(emisor, "jprewards.admin")) {
            return true;
        }
        Validador.Resultado resultado = plugin.validador().validar();
        plugin.mensajes().enviar(emisor, "comando.validar-titulo");
        for (String error : resultado.errores()) {
            plugin.mensajes().enviar(emisor, "comando.validar-error", Mensajes.ph("texto", error));
        }
        for (String aviso : resultado.avisos()) {
            plugin.mensajes().enviar(emisor, "comando.validar-aviso", Mensajes.ph("texto", aviso));
        }
        if (plugin.ajustes().debug()) {
            for (String correcto : resultado.correctos()) {
                plugin.mensajes().enviar(emisor, "comando.validar-correcto", Mensajes.ph("texto", correcto));
            }
        }
        Map<String, String> ph = Mensajes.ph("errores", String.valueOf(resultado.errores().size()),
                "avisos", String.valueOf(resultado.avisos().size()));
        plugin.mensajes().enviar(emisor, resultado.limpio() ? "comando.validar-ok" : "comando.validar-resumen", ph);
        return true;
    }

    private boolean recargar(CommandSender emisor, String[] args) {
        if (!esAdmin(emisor, "jprewards.admin")) {
            return true;
        }
        String que = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "all";
        if (!DESTINOS_RECARGA.contains(que)) {
            plugin.mensajes().enviar(emisor, "comando.recarga-destinos",
                    Mensajes.ph("destinos", String.join(", ", DESTINOS_RECARGA)));
            return true;
        }
        plugin.recargar(que);
        plugin.mensajes().enviar(emisor, "comando.recargado", Mensajes.ph("archivos", que));
        return true;
    }

    private boolean datos(CommandSender emisor, String[] args) {
        if (!esAdmin(emisor, "jprewards.admin")) {
            return true;
        }
        if (args.length < 2) {
            plugin.mensajes().enviar(emisor, "comando.datos-uso");
            return true;
        }
        OfflinePlayer objetivo = buscarJugador(args[1]);
        if (objetivo == null) {
            plugin.mensajes().enviar(emisor, "general.jugador-desconocido", Mensajes.ph("jugador", args[1]));
            return true;
        }
        DatosJugador datos = plugin.datos().obtener(objetivo.getUniqueId(), objetivo.getName());
        datos.asegurarMes(Tiempo.claveMes(plugin.ajustes().horaReinicio()));
        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("jugador", nombre(objetivo));
        ph.put("uuid", objetivo.getUniqueId().toString());
        ph.put("racha", String.valueOf(datos.racha()));
        ph.put("mejor", String.valueOf(datos.mejorRacha()));
        ph.put("total", String.valueOf(datos.total()));
        ph.put("racha_semanal", String.valueOf(datos.rachaSemanal()));
        ph.put("mejor_semanal", String.valueOf(datos.mejorRachaSemanal()));
        ph.put("total_semanal", String.valueOf(datos.totalSemanal()));
        ph.put("dias_mes", String.valueOf(datos.diasMes()));
        ph.put("hitos", String.valueOf(plugin.recompensas().hitosReclamados(datos)));
        ph.put("hitos_total", String.valueOf(plugin.recompensas().hitos().size()));
        ph.put("bonus", String.valueOf(plugin.recompensas().bonusReclamados(datos)));
        ph.put("bonus_total", String.valueOf(plugin.recompensas().bonus().size()));
        ph.put("pendientes", String.valueOf(datos.pendientes().size()));
        ph.put("ultimo", Tiempo.fechaHora(datos.ultimoReclamo()));
        ph.put("primero", Tiempo.fechaHora(datos.primerReclamo()));
        ph.put("proximo", String.valueOf(plugin.recompensas().proximoDia(datos)));
        ph.put("semana", String.valueOf(plugin.recompensas().proximaSemana(datos)));
        plugin.mensajes().enviar(emisor, "comando.datos-titulo", ph);
        plugin.mensajes().enviarLineas(emisor, "comando.datos-lineas", ph);
        for (String linea : ultimasEntregas(nombre(objetivo), 5)) {
            emisor.sendMessage(plugin.mensajes().texto("comando.datos-historial-linea", Mensajes.ph("linea", linea), null));
        }
        return true;
    }

    /** Ultimas lineas del historial de ese jugador (para /rewards data). */
    private java.util.List<String> ultimasEntregas(String jugador, int cuantas) {
        java.util.List<String> encontradas = new java.util.ArrayList<String>();
        java.io.File archivo = new java.io.File(plugin.getDataFolder(), "historial.log");
        if (!archivo.isFile()) {
            return encontradas;
        }
        try {
            for (String linea : java.nio.file.Files.readAllLines(archivo.toPath(),
                    java.nio.charset.StandardCharsets.UTF_8)) {
                if (linea.contains(" | " + jugador + " | ")) {
                    encontradas.add(linea);
                }
            }
        } catch (java.io.IOException ignorado) {
            return encontradas;
        }
        if (encontradas.size() > cuantas) {
            return encontradas.subList(encontradas.size() - cuantas, encontradas.size());
        }
        return encontradas;
    }

    private boolean dar(CommandSender emisor, String[] args) {
        if (!esAdmin(emisor, "jprewards.admin.manage")) {
            return true;
        }
        if (args.length < 3) {
            plugin.mensajes().enviar(emisor, "comando.dar-uso");
            return true;
        }
        Player destino = Bukkit.getPlayerExact(args[1]);
        if (destino == null) {
            plugin.mensajes().enviar(emisor, "general.jugador-desconectado", Mensajes.ph("jugador", args[1]));
            return true;
        }
        String tipo = args[2].toLowerCase(Locale.ROOT);
        String clave = args.length >= 4 ? args[3] : "";
        if (!TIPOS.contains(tipo)) {
            plugin.mensajes().enviar(emisor, "comando.reclamar-tipos", Mensajes.ph("tipos", String.join(", ", TIPOS)));
            return true;
        }
        Recompensa recompensa = plugin.recompensas().buscar(modulo(tipo), clave);
        if (recompensa == null || !plugin.recompensas().regalar(destino, modulo(tipo), clave)) {
            plugin.mensajes().enviar(emisor, "comando.dar-desconocido",
                    Mensajes.ph("tipo", tipo, "clave", clave.isEmpty() ? "-" : clave));
            return true;
        }
        plugin.mensajes().enviar(emisor, "comando.dar-hecho", Mensajes.ph("jugador", destino.getName(), "tipo", tipo,
                "clave", clave.isEmpty() ? "1" : clave));
        return true;
    }

    /**
     * /rewards backup: comprime la carpeta de datos en backups/ para poder
     * guardar una copia antes de tocar algo. No toca la base de datos (para eso
     * esta el backup del propio MySQL).
     */
    private boolean copiaSeguridad(CommandSender emisor) {
        if (!esAdmin(emisor, "jprewards.admin")) {
            return true;
        }
        java.io.File carpeta = new java.io.File(plugin.getDataFolder(), "datos");
        java.io.File[] archivos = carpeta.isDirectory() ? carpeta.listFiles() : null;
        if (archivos == null || archivos.length == 0) {
            plugin.mensajes().enviar(emisor, "comando.backup-vacio");
            return true;
        }
        java.io.File salida = new java.io.File(plugin.getDataFolder(), "backups");
        salida.mkdirs();
        String nombre = "datos-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss")
                .format(new java.util.Date()) + ".zip";
        java.io.File destino = new java.io.File(salida, nombre);
        int copiados = 0;
        try {
            java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(
                    new java.io.FileOutputStream(destino));
            try {
                byte[] buffer = new byte[8192];
                for (java.io.File archivo : archivos) {
                    if (!archivo.isFile()) {
                        continue;
                    }
                    zip.putNextEntry(new java.util.zip.ZipEntry(archivo.getName()));
                    java.io.FileInputStream entrada = new java.io.FileInputStream(archivo);
                    try {
                        int leidos;
                        while ((leidos = entrada.read(buffer)) > 0) {
                            zip.write(buffer, 0, leidos);
                        }
                    } finally {
                        entrada.close();
                    }
                    zip.closeEntry();
                    copiados++;
                }
            } finally {
                zip.close();
            }
        } catch (java.io.IOException error) {
            plugin.mensajes().enviar(emisor, "general.error");
            plugin.getLogger().warning("Could not write the backup: " + error.getMessage());
            return true;
        }
        plugin.mensajes().enviar(emisor, "comando.backup-hecho",
                Mensajes.ph("archivo", "backups/" + nombre, "cantidad", String.valueOf(copiados)));
        return true;
    }

    /**
     * /rewards trigger <jugador> <id>: entrega un premio suelto de
     * recompensas/extra.yml. Es la puerta que usan otros plugins (o un comando
     * desde consola) para dar una recompensa fuera de los cuatro ciclos.
     */
    private boolean disparar(CommandSender emisor, String[] args) {
        if (!esAdmin(emisor, "jprewards.admin.manage")) {
            return true;
        }
        if (args.length < 2) {
            plugin.mensajes().enviar(emisor, "comando.trigger-uso",
                    Mensajes.ph("claves", plugin.recompensas().extrasDisponibles().isEmpty()
                            ? "(recompensas/extra.yml)" : String.join(", ", plugin.recompensas().extrasDisponibles())));
            return true;
        }
        String id = args.length >= 3 ? args[2] : args[1];
        Player destino = args.length >= 3 ? Bukkit.getPlayerExact(args[1]) : (emisor instanceof Player ? (Player) emisor : null);
        if (destino == null) {
            plugin.mensajes().enviar(emisor, "general.jugador-desconectado", Mensajes.ph("jugador", args[1]));
            return true;
        }
        if (!plugin.recompensas().regalarExtra(destino, id)) {
            plugin.mensajes().enviar(emisor, "comando.trigger-desconocido",
                    Mensajes.ph("clave", id, "claves", plugin.recompensas().extrasDisponibles().isEmpty()
                            ? "(recompensas/extra.yml)" : String.join(", ", plugin.recompensas().extrasDisponibles())));
            return true;
        }
        plugin.mensajes().enviar(emisor, "comando.trigger-hecho",
                Mensajes.ph("jugador", destino.getName(), "clave", id));
        return true;
    }

    private boolean test(CommandSender emisor, String[] args) {
        if (!esAdmin(emisor, "jprewards.admin.manage")) {
            return true;
        }
        if (args.length < 2) {
            plugin.mensajes().enviar(emisor, "comando.test-uso");
            return true;
        }
        String tipo = args[1].toLowerCase(Locale.ROOT);
        if (!TIPOS.contains(tipo) || "all".equals(tipo)) {
            plugin.mensajes().enviar(emisor, "comando.reclamar-tipos", Mensajes.ph("tipos", String.join(", ", TIPOS)));
            return true;
        }
        String clave = args.length >= 3 ? args[2] : "";
        Recompensa recompensa = plugin.recompensas().buscar(modulo(tipo), clave);
        if (recompensa == null) {
            plugin.mensajes().enviar(emisor, "comando.dar-desconocido",
                    Mensajes.ph("tipo", tipo, "clave", clave.isEmpty() ? "-" : clave));
            return true;
        }
        Player jugador = emisor instanceof Player ? (Player) emisor : null;
        Recompensa.Variante variante = recompensa.variantePara(jugador);

        Map<String, String> ph = new LinkedHashMap<String, String>();
        ph.put("tipo", tipo);
        ph.put("clave", clave.isEmpty() ? "1" : clave);
        ph.put("recompensa", recompensa.nombrePara(jugador));
        ph.put("entrega", Recompensa.describir(variante));
        plugin.mensajes().enviar(emisor, "comando.test-titulo", ph);
        if (variante.comandos().isEmpty() && variante.items().isEmpty() && variante.experiencia() <= 0
                && variante.niveles() <= 0) {
            plugin.mensajes().enviar(emisor, "comando.test-vacio");
        }
        for (String comandos : variante.comandos()) {
            plugin.mensajes().enviar(emisor, "comando.test-comando", Mensajes.ph("texto", comandos));
        }
        for (ItemPremio premio : variante.items()) {
            plugin.mensajes().enviar(emisor, "comando.test-item", Mensajes.ph("texto", premio.describir()));
        }
        if (variante.experiencia() > 0) {
            plugin.mensajes().enviar(emisor, "comando.test-experiencia",
                    Mensajes.ph("texto", variante.experiencia() + " puntos"));
        }
        if (variante.niveles() > 0) {
            plugin.mensajes().enviar(emisor, "comando.test-experiencia",
                    Mensajes.ph("texto", variante.niveles() + " niveles"));
        }
        for (String mensaje : variante.efectos().mensajes()) {
            plugin.mensajes().enviar(emisor, "comando.test-mensaje", Mensajes.ph("texto", mensaje));
        }
        if (variante.efectos().sonido() != null) {
            plugin.mensajes().enviar(emisor, "comando.test-sonido", Mensajes.ph("texto", variante.efectos().sonido()));
        }
        if (variante.efectos().broadcast() != null) {
            plugin.mensajes().enviar(emisor, "comando.test-broadcast", Mensajes.ph("texto", variante.efectos().broadcast()));
        }
        plugin.mensajes().enviar(emisor, "comando.test-pie");
        return true;
    }

    private boolean reset(CommandSender emisor, String[] args) {
        if (!esAdmin(emisor, "jprewards.admin.manage")) {
            return true;
        }
        if (args.length < 2) {
            plugin.mensajes().enviar(emisor, "comando.reset-uso");
            return true;
        }
        String tipo = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "all";
        if (!TIPOS.contains(tipo)) {
            plugin.mensajes().enviar(emisor, "comando.reset-tipo-invalido", Mensajes.ph("tipos", String.join(", ", TIPOS)));
            return true;
        }
        if ("*".equals(args[1])) {
            int total = 0;
            for (java.util.UUID uuid : plugin.datos().conocidos()) {
                DatosJugador datos = plugin.datos().obtener(uuid);
                datos.reiniciar(modulo(tipo));
                plugin.datos().guardar(datos);
                total++;
            }
            plugin.mensajes().enviar(emisor, "comando.reset-todos",
                    Mensajes.ph("cantidad", String.valueOf(total), "tipo", tipo));
            plugin.estadisticas().limpiar();
            return true;
        }
        OfflinePlayer objetivo = buscarJugador(args[1]);
        if (objetivo == null) {
            plugin.mensajes().enviar(emisor, "general.jugador-desconocido", Mensajes.ph("jugador", args[1]));
            return true;
        }
        DatosJugador datos = plugin.datos().obtener(objetivo.getUniqueId(), objetivo.getName());
        datos.reiniciar(modulo(tipo));
        plugin.datos().guardar(datos);
        plugin.estadisticas().limpiar();
        plugin.mensajes().enviar(emisor, "comando.reset-hecho",
                Mensajes.ph("jugador", nombre(objetivo), "tipo", tipo));
        return true;
    }

    private boolean set(CommandSender emisor, String[] args) {
        if (!esAdmin(emisor, "jprewards.admin.manage")) {
            return true;
        }
        if (args.length < 4) {
            plugin.mensajes().enviar(emisor, "comando.set-uso", Mensajes.ph("campos", String.join(", ", CAMPOS)));
            return true;
        }
        OfflinePlayer objetivo = buscarJugador(args[1]);
        if (objetivo == null) {
            plugin.mensajes().enviar(emisor, "general.jugador-desconocido", Mensajes.ph("jugador", args[1]));
            return true;
        }
        String campo = args[2].toLowerCase(Locale.ROOT);
        int valor;
        try {
            valor = Integer.parseInt(args[3]);
        } catch (NumberFormatException error) {
            plugin.mensajes().enviar(emisor, "comando.numero-invalido", Mensajes.ph("valor", args[3]));
            return true;
        }
        DatosJugador datos = plugin.datos().obtener(objetivo.getUniqueId(), objetivo.getName());
        if ("streak".equals(campo)) {
            datos.racha(valor);
        } else if ("best-streak".equals(campo)) {
            datos.mejorRacha(valor);
        } else if ("total".equals(campo)) {
            datos.total(valor);
        } else if ("weekly-streak".equals(campo)) {
            datos.rachaSemanal(valor);
        } else if ("weekly-total".equals(campo)) {
            datos.totalSemanal(valor);
        } else if ("month-days".equals(campo)) {
            datos.diasMes(valor);
        } else if ("milestones".equals(campo)) {
            datos.hitosHistoricos(valor);
        } else {
            plugin.mensajes().enviar(emisor, "comando.set-campo-invalido", Mensajes.ph("campos", String.join(", ", CAMPOS)));
            return true;
        }
        plugin.datos().guardar(datos);
        plugin.mensajes().enviar(emisor, "comando.set-hecho",
                Mensajes.ph("jugador", nombre(objetivo), "campo", campo, "valor", String.valueOf(valor)));
        return true;
    }

    private boolean migrar(CommandSender emisor, String[] args) {
        if (!esAdmin(emisor, "jprewards.admin.migrate")) {
            return true;
        }
        boolean confirmar = args.length >= 2 && ("confirm".equalsIgnoreCase(args[1])
                || "force".equalsIgnoreCase(args[1]));
        if (!confirmar) {
            plugin.mensajes().enviar(emisor, "comando.migrar-uso");
            return true;
        }
        plugin.migrador().migrar(emisor, true);
        return true;
    }

    // ------------------------------------------------------------------
    // Ayuda y utilidades
    // ------------------------------------------------------------------

    private void enviarAyuda(CommandSender emisor, String sub) {
        plugin.mensajes().enviar(emisor, "comando.ayuda-encabezado",
                Mensajes.ph("version", plugin.getDescription().getVersion()));
        plugin.mensajes().enviarLineas(emisor, "comando.ayuda-jugador", (Map<String, String>) null);
        if (emisor.hasPermission("jprewards.admin")) {
            emisor.sendMessage("");
            plugin.mensajes().enviarLineas(emisor, "comando.ayuda-admin", (Map<String, String>) null);
        }
        if (emisor.hasPermission("jprewards.admin.manage")) {
            emisor.sendMessage("");
            plugin.mensajes().enviarLineas(emisor, "comando.ayuda-gestion", (Map<String, String>) null);
        }
        plugin.mensajes().enviar(emisor, "comando.ayuda-pie",
                Mensajes.ph("version", plugin.getDescription().getVersion()));
    }

    private boolean esAdmin(CommandSender emisor, String permiso) {
        if (!emisor.hasPermission(permiso)) {
            plugin.mensajes().enviar(emisor, "general.sin-permiso");
            return false;
        }
        return true;
    }

    private OfflinePlayer buscarJugador(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        Player enLinea = Bukkit.getPlayerExact(nombre);
        if (enLinea != null) {
            return enLinea;
        }
        OfflinePlayer jugador = Bukkit.getOfflinePlayer(nombre);
        if (jugador == null) {
            return null;
        }
        if (!jugador.hasPlayedBefore() && !jugador.isOnline() && !plugin.datos().conocidos().contains(jugador.getUniqueId())) {
            return null;
        }
        return jugador;
    }

    private String nombre(OfflinePlayer jugador) {
        String nombre = jugador.getName();
        return nombre == null ? jugador.getUniqueId().toString().substring(0, 8) : nombre;
    }

    // ------------------------------------------------------------------
    // Tab complete
    // ------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender emisor, Command comando, String etiqueta, String[] args) {
        List<String> sugerencias = new ArrayList<String>();
        if (args.length == 1) {
            for (String sub : SUBCOMANDOS) {
                if (esSoloAdmin(sub) && !tienePermiso(emisor, sub)) {
                    continue;
                }
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    sugerencias.add(sub);
                }
            }
            return sugerencias;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("claim".equals(sub) || "test".equals(sub) || "simulate".equals(sub)) {
                if (varianteNoEsSimulacro(sub) || emisor.hasPermission("jprewards.admin.manage")) {
                    for (String tipo : TIPOS) {
                        if (tipo.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                            sugerencias.add(tipo);
                        }
                    }
                }
                return sugerencias;
            }
            if ("top".equals(sub) || "leaderboard".equals(sub) || "ranking".equals(sub)) {
                for (String tipo : Estadisticas.TIPOS) {
                    if (tipo.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                        sugerencias.add(tipo);
                    }
                }
                return sugerencias;
            }
            if ("reload".equals(sub)) {
                for (String destino : DESTINOS_RECARGA) {
                    if (destino.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                        sugerencias.add(destino);
                    }
                }
                return sugerencias;
            }
            if ("reset".equals(sub) || "data".equals(sub) || "give".equals(sub) || "set".equals(sub)) {
                if (tienePermiso(emisor, sub)) {
                    if ("*".startsWith(args[1])) {
                        sugerencias.add("*");
                    }
                    for (Player jugador : Bukkit.getOnlinePlayers()) {
                        if (jugador.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                            sugerencias.add(jugador.getName());
                        }
                    }
                }
                return sugerencias;
            }
            if ("trigger".equals(sub) || "extra".equals(sub)) {
                if (emisor.hasPermission("jprewards.admin.manage")) {
                    for (Player jugador : Bukkit.getOnlinePlayers()) {
                        if (jugador.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                            sugerencias.add(jugador.getName());
                        }
                    }
                }
                return sugerencias;
            }
            if ("migrate".equals(sub) && emisor.hasPermission("jprewards.admin.migrate")) {
                if ("confirm".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    sugerencias.add("confirm");
                }
                return sugerencias;
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String valor = args[2].toLowerCase(Locale.ROOT);
            if ("reset".equals(sub) || "give".equals(sub) || "test".equals(sub) || "simulate".equals(sub)) {
                for (String tipo : TIPOS) {
                    if (tipo.startsWith(valor)) {
                        sugerencias.add(tipo);
                    }
                }
                return sugerencias;
            }
            if ("set".equals(sub)) {
                for (String campo : CAMPOS) {
                    if (campo.startsWith(valor)) {
                        sugerencias.add(campo);
                    }
                }
                return sugerencias;
            }
            if ("trigger".equals(sub) || "extra".equals(sub)) {
                for (String id : plugin.recompensas().extrasDisponibles()) {
                    if (id.startsWith(valor)) {
                        sugerencias.add(id);
                    }
                }
                return sugerencias;
            }
        }
        if (args.length == 4 && ("give".equals(args[0].toLowerCase(Locale.ROOT))
                || "dar".equals(args[0].toLowerCase(Locale.ROOT)))) {
            for (String clave : claves(args[2])) {
                if (clave.startsWith(args[3].toLowerCase(Locale.ROOT))) {
                    sugerencias.add(clave);
                }
            }
        }
        return sugerencias;
    }

    /** Claves disponibles de un tipo (dias, semanas, ids de hito o bonus). */
    private List<String> claves(String tipo) {
        List<String> claves = new ArrayList<String>();
        String limpio = tipo == null ? "" : tipo.toLowerCase(Locale.ROOT);
        if ("daily".equals(limpio)) {
            for (int i = 1; i <= plugin.recompensas().diasDelCiclo(); i++) {
                claves.add(String.valueOf(i));
            }
        } else if ("weekly".equals(limpio)) {
            for (int i = 1; i <= plugin.recompensas().semanasDelCiclo(); i++) {
                claves.add(String.valueOf(i));
            }
        } else if ("monthly".equals(limpio)) {
            for (Hito hito : plugin.recompensas().hitos()) {
                claves.add(hito.id());
            }
        } else if ("streaks".equals(limpio)) {
            for (BonusRacha bono : plugin.recompensas().bonus()) {
                claves.add(bono.id());
            }
        }
        return claves;
    }

    /** true si el subcomando no es el simulacro (que necesita permiso de gestion). */
    private boolean varianteNoEsSimulacro(String sub) {
        return !"test".equals(sub) && !"simulate".equals(sub) && !"simular".equals(sub) && !"simulacro".equals(sub);
    }

    /** Subcomandos que cualquier jugador puede usar. */
    private boolean esSoloAdmin(String sub) {
        return !Arrays.asList("help", "ayuda", "daily", "diaria", "weekly", "semanal", "monthly", "mensual",
                "streaks", "rachas", "info", "claim", "reclamar", "top", "ranking").contains(sub);
    }

    private boolean tienePermiso(CommandSender emisor, String sub) {
        if (Arrays.asList("give", "dar", "test", "simulate", "simular", "reset", "reiniciar", "set", "fijar")
                .contains(sub)) {
            return emisor.hasPermission("jprewards.admin.manage");
        }
        if ("migrar".equals(sub) || "migrate".equals(sub)) {
            return emisor.hasPermission("jprewards.admin.migrate");
        }
        return emisor.hasPermission("jprewards.admin");
    }

    /** Carpeta del plugin relativa al servidor, para el estado. */
    private String rutaRelativa() {
        try {
            java.nio.file.Path raiz = plugin.getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
            return raiz.relativize(plugin.getDataFolder().toPath().toAbsolutePath().normalize())
                    .toString().replace('\\', '/');
        } catch (Throwable error) {
            return plugin.getDataFolder().getPath();
        }
    }

    /**
     * Traduce el tipo que escribe el jugador ('daily', 'weekly', 'monthly',
     * 'streaks', 'all') al nombre interno del modulo, que es el que usan los
     * datos y los mensajes ('diaria', 'semanal', 'mensual', 'rachas', 'todo').
     */
    private static String modulo(String tipo) {
        if ("daily".equals(tipo)) {
            return "diaria";
        }
        if ("weekly".equals(tipo)) {
            return "semanal";
        }
        if ("monthly".equals(tipo)) {
            return "mensual";
        }
        if ("streaks".equals(tipo)) {
            return "rachas";
        }
        return "todo";
    }

    /** Texto plano de un mensaje (para reutilizarlo en la consola). */
    public String plano(String clave) {
        return Texto.plano(plugin.mensajes().texto(clave));
    }
}
