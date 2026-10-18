package me.jp.jprewards.config;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.model.BonusRacha;
import me.jp.jprewards.model.Hito;
import me.jp.jprewards.model.ItemPremio;
import me.jp.jprewards.model.Recompensa;
import me.jp.jprewards.util.Compat;
import me.jp.jprewards.util.Tiempo;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Revisa TODA la configuracion y explica, en castellano y con la ruta exacta,
 * que esta mal y como arreglarlo. Se ejecuta a peticion con
 * /rewards validate (y sus resultados tambien salen en /rewards info).
 *
 * Comprueba: YAML legibles, claves que falten o sobren respecto a los
 * archivos incluidos en el .jar, materiales/sonidos/particulas/encantamientos
 * que no existan en esta version, slots fuera del cofre, acciones de menu que
 * apunten a menus o hitos inexistentes, recompensas sin premios, texturas
 * invalidas y placeholders desconocidos.
 */
public final class Validador {

    /** Placeholders que el plugin resuelve en menus.yml y messages_*.yml. */
    private static final Set<String> PLACEHOLDERS = new HashSet<String>(Arrays.asList(
            "jugador", "uuid", "mundo", "premio", "recompensa", "dia", "dias", "faltan", "estado", "cantidad",
            "racha", "mejor", "total", "racha_minima", "racha_semanal", "mejor_semanal", "total_semanal",
            "semana", "semanas", "dias_mes", "dias_mes_total", "hitos", "hitos_total", "hitos_disponibles",
            "bonus", "bonus_total", "bonus_disponibles", "pendientes", "tiempo", "tiempo_largo", "tiempo_semanal",
            "hora", "moneda", "barra_mes", "barra_ciclo", "barra_racha", "barra_racha_semanal",
            "version", "servidor", "almacenamiento", "modulos", "ultimo", "primero", "top", "pagina", "paginas",
            "preview", "prefijo",
            // Placeholders de los mensajes del comando y de los avisos.
            "modulo", "item", "actual", "entrada", "archivos", "texto", "tipos", "campos", "clave", "valor",
            "puesto", "errores", "avisos", "copiados", "omitidos", "origen", "destino", "error", "hito",
            "proximo", "racha_necesaria", "tipo", "ruta", "cargados", "autoguardado", "tipo_almacenamiento",
            "dias_requeridos", "premio_diaria", "premio_semanal", "faltan_dias", "jugador_uuid",
            "idioma", "campo", "campos", "destinos"));

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

    private final JPRewards plugin;

    public Validador(JPRewards plugin) {
        this.plugin = plugin;
    }

    /** Resultado de una revision. */
    public static final class Resultado {

        private final List<String> errores = new ArrayList<String>();
        private final List<String> avisos = new ArrayList<String>();
        private final List<String> correctos = new ArrayList<String>();

        public void error(String texto) {
            errores.add(texto);
        }

        public void aviso(String texto) {
            avisos.add(texto);
        }

        public void correcto(String texto) {
            correctos.add(texto);
        }

        public List<String> errores() {
            return new ArrayList<String>(errores);
        }

        public List<String> avisos() {
            return new ArrayList<String>(avisos);
        }

        public List<String> correctos() {
            return new ArrayList<String>(correctos);
        }

        public boolean limpio() {
            return errores.isEmpty();
        }

        public int total() {
            return errores.size() + avisos.size() + correctos.size();
        }
    }

    /** Revision completa. */
    public Resultado validar() {
        Resultado resultado = new Resultado();
        validarConfig(resultado);
        validarMensajes(resultado);
        validarMenus(resultado);
        validarRecompensas(resultado);
        return resultado;
    }

    // ------------------------------------------------------------------
    // config.yml
    // ------------------------------------------------------------------

    private void validarConfig(Resultado resultado) {
        FileConfiguration config = plugin.ajustes().crudo();
        if (config == null) {
            resultado.error("config.yml could not be read. Delete it and restart the server to recreate it.");
            return;
        }
        resultado.correcto("config.yml reads correctly (version " + plugin.ajustes().version() + ").");
        List<String> problemas = plugin.ajustes().problemas();
        for (String problema : problemas) {
            resultado.aviso(problema);
        }
        if (!plugin.ajustes().sonidosActivos()) {
            resultado.aviso("config.yml -> sonidos.activado esta en false: el plugin no reproducira sonidos.");
        }
        if (plugin.ajustes().almacenTipo() == me.jp.jprewards.storage.TipoAlmacen.MYSQL
                && plugin.datos().tipoReal() != me.jp.jprewards.storage.TipoAlmacen.MYSQL) {
            resultado.error("almacenamiento.tipo es 'mysql' pero el plugin esta usando YAML (la base no respondio)."
                    + " Check almacenamiento.mysql y mira la consola al arrancar.");
        }
        compararClaves(resultado, "config.yml", "config.yml");
    }

    // ------------------------------------------------------------------
    // messages_<idioma>.yml
    // ------------------------------------------------------------------

    private void validarMensajes(Resultado resultado) {
        List<String> faltantes = plugin.mensajes().clavesFaltantes();
        if (faltantes.isEmpty()) {
            resultado.correcto("messages_" + plugin.mensajes().idioma() + ".yml has every key of the base language.");
        } else {
            resultado.aviso("messages_" + plugin.mensajes().idioma() + ".yml no tiene " + faltantes.size()
                    + " key(s) of the base language; the texts bundled in the .jar will be used: "
                    + resumir(faltantes));
        }
        List<String> obsoletas = plugin.mensajes().clavesObsoletas();
        if (!obsoletas.isEmpty()) {
            resultado.aviso("messages_" + plugin.mensajes().idioma() + ".yml tiene " + obsoletas.size()
                    + " key(s) that are no longer used (harmless, you can delete them): " + resumir(obsoletas));
        }
        revisarPlaceholders(resultado, "messages_" + plugin.mensajes().idioma() + ".yml", plugin.mensajes().archivo());
    }

    // ------------------------------------------------------------------
    // menus.yml
    // ------------------------------------------------------------------

    /**
     * Botones que el propio plugin reparte por el cofre (los items de los
     * ciclos y de los hitos usan 'slot-inicial' o 'slots' del menu), asi que
     * no necesitan 'slot' y no hay que avisar de ello.
     */
    private static boolean esBotonAutomatico(String clave) {
        return "dia".equals(clave) || "semana".equals(clave) || "hito".equals(clave) || "bonus".equals(clave);
    }

    private void validarMenus(Resultado resultado) {
        FileConfiguration menus = plugin.menus().archivo();
        String archivo = plugin.ajustes().menusArchivo();
        if (menus == null) {
            resultado.error(archivo + " could not be read. Delete it and restart the server to recreate it.");
            return;
        }
        List<String> definidos = plugin.menus().menus();
        if (definidos.isEmpty()) {
            resultado.error(archivo + " defines no menu (sections 'menu-principal', 'menu-diario'... were expected).");
        } else {
            resultado.correcto(archivo + " define " + definidos.size() + " menu(s): " + String.join(", ", definidos));
        }
        for (String menuId : definidos) {
            int filas = plugin.menus().filas(menuId);
            int tamano = filas * 9;
            if (plugin.menus().titulo(menuId).isEmpty()) {
                resultado.aviso(archivo + " -> " + menuId + ": 'titulo' esta vacio.");
            }
            for (String clave : plugin.menus().botones(menuId)) {
                ItemDef def = plugin.menus().boton(menuId, clave);
                if (def == null) {
                    continue;
                }
                String ruta = menuId + ".botones." + clave;
                List<Integer> slots = def.slots(tamano);
                if (slots.isEmpty() && !esBotonAutomatico(clave)) {
                    resultado.aviso(archivo + " -> " + ruta + ": has no 'slot' (or it is outside the chest of "
                            + tamano + " slots), so it will not be shown.");
                }
                if (!def.textura("").isEmpty()) {
                    String motivo = me.jp.jprewards.util.Cabezas.validarTextura(def.textura(""));
                    if (motivo != null) {
                        resultado.error(archivo + " -> " + ruta + ".textura: " + motivo + ".");
                    }
                }
                if (def.variante("material") != null && !def.textura("").isEmpty()
                        && !def.texturaJugador("").isEmpty()) {
                    resultado.aviso(archivo + " -> " + ruta + ": tiene 'textura' y 'textura-jugador'; using la textura.");
                }
                if (def.variante("material") != null && def.textura("").isEmpty()
                        && !Compat.materialExiste(def.material())) {
                    resultado.error(archivo + " -> " + ruta + ".material: '" + def.material()
                            + "' does not exist on this Minecraft version.");
                }
                for (String encantamiento : def.encantamientos().keySet()) {
                    if (!Compat.encantamientoExiste(encantamiento)) {
                        resultado.error(archivo + " -> " + ruta + ".encantamientos: '" + encantamiento
                                + "' does not exist on this version.");
                    }
                }
                for (String flag : def.flags()) {
                    if (Compat.flag(flag) == null) {
                        resultado.aviso(archivo + " -> " + ruta + ".item-flags: '" + flag + "' does not exist on this version.");
                    }
                }
                revisarAcciones(resultado, archivo, ruta, def);
            }
        }
        int volverSlot = plugin.menus().navegacion("menu-principal", "volver-slot", 0);
        if (volverSlot != 0) {
            resultado.aviso(archivo + " -> menu-principal.navegacion.volver-slot: the main menu has nowhere to go back to"
                    + " (use 0 to hide it).");
        }
        revisarPlaceholders(resultado, archivo, menus);
    }

    private void revisarAcciones(Resultado resultado, String archivo, String ruta, ItemDef def) {
        List<String> acciones = def.acciones(null);
        for (String accion : acciones) {
            // Se aceptan tanto los nombres nuevos (open, close, none, claim...)
            // como los de la 1.x (abrir, cerrar, nada, reclamar...).
            String limpia = me.jp.jprewards.menu.MenuManager.normalizarAccion(accion.trim());
            if (limpia.startsWith("open:")) {
                String destino = plugin.menuManager().normalizarId(limpia.substring("open:".length()).trim());
                if (destino.isEmpty()) {
                    resultado.error(archivo + " -> " + ruta + ".accion: 'open:' does not say which menu to open.");
                } else if (!plugin.menus().existe(destino)) {
                    resultado.error(archivo + " -> " + ruta + ".accion: the menu '" + destino + "' does not exist in "
                            + archivo + " (check the exact section name).");
                }
                continue;
            }
            if ("close".equals(limpia) || "none".equals(limpia) || limpia.isEmpty()) {
                continue;
            }
            if (limpia.startsWith("claim:")) {
                String que = limpia.substring("claim:".length()).trim();
                if (que.startsWith("milestone:")) {
                    String id = que.substring("milestone:".length()).trim();
                    if (plugin.recompensas().hito(id) == null) {
                        resultado.aviso(archivo + " -> " + ruta + ".accion: el hito '" + id
                                + "' does not exist in recompensas/mensuales.yml.");
                    }
                } else if (que.startsWith("streak:")) {
                    String id = que.substring("streak:".length()).trim();
                    if (plugin.recompensas().bonus(id) == null) {
                        resultado.aviso(archivo + " -> " + ruta + ".accion: el bonus '" + id
                                + "' does not exist in recompensas/rachas.yml.");
                    }
                } else if (!"daily".equals(que) && !"weekly".equals(que) && !"monthly".equals(que)
                        && !"streaks".equals(que) && !"all".equals(que)) {
                    resultado.aviso(archivo + " -> " + ruta + ".accion: '" + que
                            + "' is not a valid reward type (daily, weekly, monthly, streaks, all).");
                }
                continue;
            }
            if (limpia.startsWith("command:") || limpia.startsWith("player-command:")
                    || limpia.startsWith("message:") || limpia.startsWith("sound:") || limpia.startsWith("page:")) {
                continue;
            }
            resultado.aviso(archivo + " -> " + ruta + ".accion: '" + limpia + "' is not recognised."
                    + " Acciones validas: open:<menu>, close, none, claim:daily|weekly|monthly|streaks|all,"
                    + " claim:milestone:<id>, claim:streak:<id>, command:<cmd>, player-command:<cmd>,"
                    + " message:<clave>, sound:<nombre>.");
        }
    }

    // ------------------------------------------------------------------
    // recompensas/*.yml
    // ------------------------------------------------------------------

    private void validarRecompensas(Resultado resultado) {
        validarCiclo(resultado, "diaria", plugin.ajustes().archivoDiarias(), plugin.recompensas().diasDelCiclo());
        validarCiclo(resultado, "semanal", plugin.ajustes().archivoSemanales(), plugin.recompensas().semanasDelCiclo());

        // Hitos mensuales
        List<Hito> hitos = plugin.recompensas().hitos();
        String archivoMensual = plugin.ajustes().archivoMensuales();
        if (hitos.isEmpty()) {
            resultado.aviso(archivoMensual + " defines no milestone; the monthly menu will be empty.");
        } else {
            resultado.correcto(archivoMensual + " define " + hitos.size() + " hito(s).");
            int maximo = Tiempo.diasDelMesActual(plugin.ajustes().horaReinicio());
            Set<Integer> dias = new LinkedHashSet<Integer>();
            for (Hito hito : hitos) {
                if (hito.diasRequeridos() > 31) {
                    resultado.error(archivoMensual + " -> hitos." + hito.id() + ".dias-requeridos: "
                            + hito.diasRequeridos() + " is impossible (a month has 31 days at most).");
                } else if (hito.diasRequeridos() > maximo) {
                    resultado.aviso(archivoMensual + " -> hitos." + hito.id() + ".dias-requeridos: "
                            + hito.diasRequeridos() + " cannot be reached this month (" + maximo + " days).");
                }
                if (!dias.add(Integer.valueOf(hito.diasRequeridos()))) {
                    resultado.aviso(archivoMensual + " -> hitos." + hito.id() + ": hay mas de un hito con "
                            + hito.diasRequeridos() + " days required.");
                }
                validarRecompensa(resultado, archivoMensual, "hitos." + hito.id(), hito.recompensa());
            }
        }

        // Bonus de racha
        List<BonusRacha> bonus = plugin.recompensas().bonus();
        String archivoRachas = plugin.ajustes().archivoRachas();
        if (bonus.isEmpty()) {
            resultado.aviso(archivoRachas + " defines no streak bonus; the streak menu will be empty.");
        } else {
            resultado.correcto(archivoRachas + " define " + bonus.size() + " bonus de racha.");
            for (BonusRacha bono : bonus) {
                if (bono.rachaMinima() <= 0) {
                    resultado.error(archivoRachas + " -> bonus." + bono.id() + ".racha-minima debe ser 1 o mas.");
                }
                validarRecompensa(resultado, archivoRachas, "bonus." + bono.id(), bono.recompensa());
            }
        }
    }

    private void validarCiclo(Resultado resultado, String tipo, String archivo, int total) {
        if (total <= 0) {
            resultado.error(archivo + " defines no entry. Check the file (the keys must be numbers).");
            return;
        }
        resultado.correcto(archivo + " define " + total + " entry(ies) for the " + tipo + ".");
        for (int i = 1; i <= total; i++) {
            Recompensa recompensa = "diaria".equals(tipo) ? plugin.recompensas().recompensaDelDia(i)
                    : plugin.recompensas().recompensaDeSemana(i);
            validarRecompensa(resultado, archivo, "(" + i + ")", recompensa);
        }
    }

    private void validarRecompensa(Resultado resultado, String archivo, String ruta, Recompensa recompensa) {
        if (recompensa == null) {
            resultado.aviso(archivo + " -> " + ruta + ": the reward could not be read.");
            return;
        }
        Recompensa.Variante variante = recompensa.varianteBase();
        if (variante.sinEntrega() && !recompensa.tieneVariantes()) {
            resultado.aviso(archivo + " -> " + ruta + ": delivers nothing (no commands, no items, no experience).");
        }
        if (variante.tieneIcono() && variante.icono() == null) {
            resultado.aviso(archivo + " -> " + ruta + ": the icon could not be created on this version.");
        }
        validarEfectos(resultado, archivo, ruta, variante);
        for (ItemPremio premio : variante.items()) {
            if (premio.crear() == null) {
                resultado.error(archivo + " -> " + ruta + ": an item of the reward could not be created (check the material).");
            }
        }
        ConfigurationSection seccion = recompensa.seccion();
        ConfigurationSection rangos = seccion == null ? null : seccion.getConfigurationSection("rangos");
        int variantes = rangos == null ? 0 : rangos.getKeys(false).size();
        if (variantes > 0 && recompensa.varianteBase().sinEntrega()) {
            resultado.aviso(archivo + " -> " + ruta + ": defines 'rangos' but the base version delivers nothing"
                    + " (quien no tenga rango no recibira nada).");
        }
    }

    private void validarEfectos(Resultado resultado, String archivo, String ruta, Recompensa.Variante variante) {
        if (variante.efectos().sonido() != null && !Compat.sonidoExiste(variante.efectos().sonido())) {
            resultado.aviso(archivo + " -> " + ruta + ".efectos.sonido: '" + variante.efectos().sonido()
                    + "' does not exist on this version (nothing will play).");
        }
        if (variante.efectos().particula() != null && !Compat.particulaExiste(variante.efectos().particula())) {
            resultado.aviso(archivo + " -> " + ruta + ".efectos.particula: '" + variante.efectos().particula()
                    + "' does not exist on this version (no particle will be shown).");
        }
    }

    // ------------------------------------------------------------------
    // Comparacion con los archivos del .jar y placeholders
    // ------------------------------------------------------------------

    private void compararClaves(Resultado resultado, String archivo, String recurso) {
        FileConfiguration usuario = plugin.archivos().cargar(archivo);
        FileConfiguration defecto = plugin.archivos().cargarRecurso(recurso);
        if (usuario == null || defecto == null) {
            return;
        }
        List<String> faltantes = new ArrayList<String>();
        for (String clave : Mensajes.recorrer(defecto, "")) {
            if (!usuario.isSet(clave)) {
                faltantes.add(clave);
            }
        }
        if (faltantes.isEmpty()) {
            resultado.correcto(archivo + " has every option of this version.");
        } else {
            resultado.aviso(archivo + " is missing " + faltantes.size() + " new option(s) (they are added"
                    + " automatically when you reload with /rewards reload): " + resumir(faltantes));
        }
    }

    private void revisarPlaceholders(Resultado resultado, String archivo, FileConfiguration configuracion) {
        if (configuracion == null) {
            return;
        }
        Set<String> desconocidos = new LinkedHashSet<String>();
        for (String clave : Mensajes.recorrer(configuracion, "")) {
            Object valor = configuracion.get(clave);
            List<String> textos = new ArrayList<String>();
            if (valor instanceof String) {
                textos.add((String) valor);
            } else if (valor instanceof List) {
                for (Object elemento : (List<?>) valor) {
                    textos.add(String.valueOf(elemento));
                }
            }
            for (String texto : textos) {
                Matcher buscador = PLACEHOLDER.matcher(texto);
                while (buscador.find()) {
                    String nombre = buscador.group(1).toLowerCase(Locale.ROOT);
                    if (!PLACEHOLDERS.contains(nombre)) {
                        desconocidos.add(nombre);
                    }
                }
            }
        }
        if (!desconocidos.isEmpty()) {
            resultado.aviso(archivo + ": there are placeholders the plugin does not fill in, so they will show as-is: "
                    + String.join(", ", desconocidos) + ". Check the placeholder list in the README.");
        }
    }

    private static String resumir(List<String> claves) {
        if (claves.size() <= 8) {
            return String.join(", ", claves);
        }
        return String.join(", ", claves.subList(0, 8)) + " (+" + (claves.size() - 8) + " mas)";
    }
}
