package me.jp.jprewards.manager;

import me.jp.jprewards.JPRewards;
import me.jp.jprewards.config.Mensajes;
import me.jp.jprewards.model.BonusRacha;
import me.jp.jprewards.model.DatosJugador;
import me.jp.jprewards.model.Hito;
import me.jp.jprewards.model.Recompensa;
import me.jp.jprewards.util.Compat;
import me.jp.jprewards.util.Texto;
import me.jp.jprewards.util.Tiempo;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nucleo del sistema de recompensas.
 *
 * Reune los cuatro ciclos (diario, semanal, mensual y bonus de racha), decide
 * que puede reclamar cada jugador, entrega los premios y envia los avisos.
 * Los menus y los comandos solo preguntan aqui: toda la logica vive en un
 * unico sitio, asi que las reglas son las mismas se reclame como se reclame
 * (menu, comando o reclamo automatico al entrar).
 */
public final class RecompensaManager {

    private final JPRewards plugin;
    private final Cargador cargador;
    private final Entrega entrega;

    /** Tiempo minimo entre dos mensajes iguales seguidos (anti-spam). */
    private static final long AVISO_MINIMO_MS = 1500L;

    private final Map<Integer, Recompensa> ciclo = new LinkedHashMap<Integer, Recompensa>();
    private final Map<Integer, Recompensa> cicloSemanal = new LinkedHashMap<Integer, Recompensa>();
    /** Premios sueltos de recompensas/extra.yml (los da la API o /rewards trigger). */
    private final Map<String, Recompensa> extras = new LinkedHashMap<String, Recompensa>();
    private final List<Hito> hitos = new ArrayList<Hito>();
    private final List<BonusRacha> bonus = new ArrayList<BonusRacha>();
    private final java.util.Set<java.util.UUID> reclamando = new java.util.HashSet<java.util.UUID>();
    private final Map<java.util.UUID, String> ultimoAviso = new HashMap<java.util.UUID, String>();
    private final Map<java.util.UUID, Long> ultimoAvisoMs = new HashMap<java.util.UUID, Long>();

    public RecompensaManager(JPRewards plugin) {
        this.plugin = plugin;
        this.cargador = new Cargador(plugin);
        this.entrega = new Entrega(plugin);
    }

    // ------------------------------------------------------------------
    // Carga
    // ------------------------------------------------------------------

    public void cargar() {
        ciclo.clear();
        cicloSemanal.clear();
        hitos.clear();
        bonus.clear();

        ciclo.putAll(cargador.cargarCiclo(plugin.ajustes().archivoDiarias(), "dias", "dia"));
        if (ciclo.isEmpty()) {
            plugin.getLogger().warning("No daily rewards loaded (check "
                    + plugin.ajustes().archivoDiarias() + ").");
        }
        int configurado = plugin.ajustes().diasCiclo();
        if (configurado > 0 && configurado != ciclo.size()) {
            plugin.getLogger().warning("config.yml -> diaria.dias-del-ciclo es " + configurado + " pero "
                    + plugin.ajustes().archivoDiarias() + " define " + ciclo.size() + " day(s); using "
                    + ciclo.size() + " (leave dias-del-ciclo at 0 so it adjusts itself).");
        }

        cicloSemanal.putAll(cargador.cargarCiclo(plugin.ajustes().archivoSemanales(), "semanas", "semana"));
        if (cicloSemanal.isEmpty()) {
            plugin.getLogger().warning("No weekly rewards loaded (check "
                    + plugin.ajustes().archivoSemanales() + ").");
        }
        int configuradoSemanal = plugin.ajustes().semanasCiclo();
        if (configuradoSemanal > 0 && configuradoSemanal != cicloSemanal.size()) {
            plugin.getLogger().warning("config.yml -> semanal.semanas-del-ciclo es " + configuradoSemanal + " pero "
                    + plugin.ajustes().archivoSemanales() + " define " + cicloSemanal.size() + " week(s); using "
                    + cicloSemanal.size() + ".");
        }

        hitos.addAll(cargador.cargarHitos(plugin.ajustes().archivoMensuales()));
        bonus.addAll(cargador.cargarBonus(plugin.ajustes().archivoRachas()));
        cargarExtras();
    }

    /**
     * Lee recompensas/extra.yml: premios con nombre que no pertenecen a ningun
     * ciclo y se entregan cuando los pide la API o /rewards trigger. Asi se
     * pueden dar recompensas desde eventos de otros plugins.
     */
    private void cargarExtras() {
        extras.clear();
        Map<String, Recompensa> leidos = cargador.cargarExtras("recompensas/extra.yml");
        extras.putAll(leidos);
        if (!extras.isEmpty()) {
            plugin.getLogger().info("Extra rewards loaded from recompensas/extra.yml: " + extras.size()
                    + " (use /rewards trigger or the API).");
        }
    }

    /** Ids de los premios sueltos definidos. */
    public List<String> extrasDisponibles() {
        return new ArrayList<String>(extras.keySet());
    }

    /** Un premio suelto por id (null si no existe). */
    public Recompensa extra(String id) {
        return id == null ? null : extras.get(id.trim());
    }

    /** Entrega un premio suelto (recompensas/extra.yml). */
    public boolean regalarExtra(Player jugador, String id) {
        Recompensa recompensa = extra(id);
        if (recompensa == null) {
            return false;
        }
        DatosJugador datos = datos(jugador);
        Verdict verdicto = condiciones(jugador, recompensa, datos);
        if (verdicto == Verdict.BLOQUEAR) {
            plugin.mensajes().enviar(jugador, "general.mundo-bloqueado", Mensajes.ph("mundo", recompensa.mundo()));
            plugin.sonidos().error(jugador);
            return false;
        }
        boolean entregado = entregarSiProcede(jugador, recompensa, recompensa.nombrePara(jugador), verdicto, datos, false);
        plugin.datos().guardar(datos);
        return entregado;
    }

    /** Claves del ciclo diario (1..N). */
    public List<Integer> diasDelCicloLista() {
        return new ArrayList<Integer>(ciclo.keySet());
    }

    /** Claves del ciclo semanal (1..N). */
    public List<Integer> semanasDelCicloLista() {
        return new ArrayList<Integer>(cicloSemanal.keySet());
    }

    // ------------------------------------------------------------------
    // Datos del jugador
    // ------------------------------------------------------------------

    /** Que hacer con una recompensa cuyas condiciones no dejan entregarla. */
    private enum Verdict {
        ENTREGAR, SALTAR, BLOQUEAR
    }

    private static final java.util.Random AZAR = new java.util.Random();

    /**
     * Evalua 'mundo', 'una-vez' y 'probabilidad'. BLOQUEAR no consume el
     * reclamo (el jugador puede intentarlo en otro mundo); SALTAR si lo
     * consume, pero no entrega nada (asi no se puede reintentar hasta sacarlo).
     */
    private Verdict condiciones(Player jugador, Recompensa recompensa, DatosJugador datos) {
        if (recompensa == null) {
            return Verdict.ENTREGAR;
        }
        if (!recompensa.mundo().isEmpty()
                && (jugador.getWorld() == null || !recompensa.mundo().equalsIgnoreCase(jugador.getWorld().getName()))) {
            return Verdict.BLOQUEAR;
        }
        if (recompensa.unaVez() && datos.yaRecibio(recompensa.id())) {
            return Verdict.SALTAR;
        }
        if (recompensa.probabilidad() >= 0.0 && AZAR.nextDouble() >= recompensa.probabilidad()) {
            return Verdict.SALTAR;
        }
        return Verdict.ENTREGAR;
    }

    /**
     * Entrega si las condiciones lo permiten. Devuelve true si se entrego.
     * Si no, avisa al jugador (si no es un reclamo silencioso).
     */
    private boolean entregarSiProcede(Player jugador, Recompensa recompensa, String nombre, Verdict verdicto,
            DatosJugador datos, boolean silencioso) {
        if (verdicto == Verdict.ENTREGAR) {
            entrega.entregar(jugador, recompensa, nombre);
            if (recompensa.unaVez()) {
                datos.marcarRecibido(recompensa.id());
            }
            return true;
        }
        if (!silencioso) {
            if (verdicto == Verdict.SALTAR) {
                String clave = recompensa.unaVez() && datos.yaRecibio(recompensa.id())
                        ? "general.ya-unico" : "general.sin-suerte";
                plugin.mensajes().enviar(jugador, clave);
                plugin.sonidos().error(jugador);
            }
        }
        return false;
    }

    /**
     * Evita que un mismo jugador tenga dos reclamos a la vez (por ejemplo si un
     * premio ejecuta un comando que vuelve a reclamar). Es una red de
     * seguridad: la comprobacion real de "ya reclamado" es la de los datos.
     */
    private boolean empezarReclamo(Player jugador) {
        return reclamando.add(jugador.getUniqueId());
    }

    private void terminarReclamo(Player jugador) {
        reclamando.remove(jugador.getUniqueId());
    }

    /**
     * true si se puede enviar un aviso a este jugador. Evita que machacar un
     * boton bloquee el chat con el mismo mensaje: como mucho uno cada 1,5 s.
     */
    private boolean puedeAvisar(Player jugador, String clave) {
        java.util.UUID id = jugador.getUniqueId();
        long ahora = System.currentTimeMillis();
        Long cuando = ultimoAvisoMs.get(id);
        if (clave.equals(ultimoAviso.get(id)) && cuando != null && (ahora - cuando.longValue()) < AVISO_MINIMO_MS) {
            return false;
        }
        ultimoAviso.put(id, clave);
        ultimoAvisoMs.put(id, Long.valueOf(ahora));
        return true;
    }

    /** Se llama al salir el jugador para no dejar mapas creciendo. */
    public void limpiarAvisos(java.util.UUID uuid) {
        ultimoAviso.remove(uuid);
        ultimoAvisoMs.remove(uuid);
    }

    /** Datos del jugador listos para usar (mes actual asegurado). */
    public DatosJugador datos(Player jugador) {
        DatosJugador datos = plugin.datos().obtener(jugador.getUniqueId(), jugador.getName());
        datos.asegurarMes(Tiempo.claveMes(plugin.ajustes().horaReinicio()));
        return datos;
    }

    // ------------------------------------------------------------------
    // Ciclo diario
    // ------------------------------------------------------------------

    public int diasDelCiclo() {
        return ciclo.isEmpty() ? Math.max(1, plugin.ajustes().diasCiclo()) : ciclo.size();
    }

    public boolean hayDiaria() {
        return plugin.ajustes().moduloDiaria() && !ciclo.isEmpty();
    }

    public Recompensa recompensaDelDia(int dia) {
        return ciclo.get(Integer.valueOf(dia));
    }

    /** Dia del ciclo que toca reclamar ahora (1..N). */
    public int proximoDia(DatosJugador datos) {
        return (datos.total() % diasDelCiclo()) + 1;
    }

    public boolean puedeReclamarDiaria(DatosJugador datos) {
        if (!hayDiaria()) {
            return false;
        }
        return Tiempo.claveDia(plugin.ajustes().horaReinicio()) > datos.ultimoDia();
    }

    public String tiempoRestante() {
        return Tiempo.restante(plugin.ajustes().horaReinicio());
    }

    // ------------------------------------------------------------------
    // Ciclo semanal
    // ------------------------------------------------------------------

    public int semanasDelCiclo() {
        return cicloSemanal.isEmpty() ? Math.max(1, plugin.ajustes().semanasCiclo()) : cicloSemanal.size();
    }

    public boolean haySemanal() {
        return plugin.ajustes().moduloSemanal() && !cicloSemanal.isEmpty();
    }

    public Recompensa recompensaDeSemana(int semana) {
        return cicloSemanal.get(Integer.valueOf(semana));
    }

    /** Bloque del ciclo semanal que toca reclamar ahora (1..N). */
    public int proximaSemana(DatosJugador datos) {
        return (datos.totalSemanal() % semanasDelCiclo()) + 1;
    }

    public boolean puedeReclamarSemanal(DatosJugador datos) {
        if (!haySemanal()) {
            return false;
        }
        return Tiempo.claveSemana(plugin.ajustes().horaReinicio(), plugin.ajustes().diaInicioSemana())
                > datos.ultimaSemana();
    }

    public String tiempoRestanteSemanal() {
        return Tiempo.restanteSemana(plugin.ajustes().horaReinicio(), plugin.ajustes().diaInicioSemana());
    }

    // ------------------------------------------------------------------
    // Hitos mensuales
    // ------------------------------------------------------------------

    public List<Hito> hitos() {
        return new ArrayList<Hito>(hitos);
    }

    public boolean hayMensual() {
        return plugin.ajustes().moduloMensual() && !hitos.isEmpty();
    }

    public Hito hito(String id) {
        for (Hito hito : hitos) {
            if (hito.id().equals(id)) {
                return hito;
            }
        }
        return null;
    }

    /** Hitos que el jugador ya puede reclamar y aun no lo hizo. */
    public List<Hito> hitosDisponibles(DatosJugador datos) {
        List<Hito> disponibles = new ArrayList<Hito>();
        if (!plugin.ajustes().moduloMensual()) {
            return disponibles;
        }
        for (Hito hito : hitos) {
            if (datos.diasMes() >= hito.diasRequeridos() && !datos.hitosReclamados().contains(hito.id())) {
                disponibles.add(hito);
            }
        }
        return disponibles;
    }

    /** Hitos del mes que el jugador ya completo (reclamados). */
    public int hitosReclamados(DatosJugador datos) {
        int total = 0;
        for (Hito hito : hitos) {
            if (datos.hitosReclamados().contains(hito.id())) {
                total++;
            }
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Bonus de racha
    // ------------------------------------------------------------------

    public List<BonusRacha> bonus() {
        return new ArrayList<BonusRacha>(bonus);
    }

    public boolean hayRachas() {
        return plugin.ajustes().moduloRachas() && !bonus.isEmpty();
    }

    public BonusRacha bonus(String id) {
        for (BonusRacha bono : bonus) {
            if (bono.id().equals(id)) {
                return bono;
            }
        }
        return null;
    }

    /** Racha que cuenta para un bonus (diaria o semanal). */
    public int rachaDe(DatosJugador datos, BonusRacha bono) {
        return bono.tipo() == BonusRacha.Tipo.SEMANAL ? datos.rachaSemanal() : datos.racha();
    }

    /** Bonus que el jugador ya puede reclamar y aun no lo hizo en esta racha. */
    public List<BonusRacha> bonusDisponibles(DatosJugador datos) {
        List<BonusRacha> disponibles = new ArrayList<BonusRacha>();
        if (!hayRachas()) {
            return disponibles;
        }
        for (BonusRacha bono : bonus) {
            if (datos.bonusReclamados().contains(bono.id())) {
                continue;
            }
            if (rachaDe(datos, bono) >= bono.rachaMinima()) {
                disponibles.add(bono);
            }
        }
        return disponibles;
    }

    public int bonusReclamados(DatosJugador datos) {
        return datos.bonusReclamados().size();
    }

    // ------------------------------------------------------------------
    // Reclamos
    // ------------------------------------------------------------------

    /** Reclama la recompensa diaria. Devuelve true si se entrego. */
    public boolean reclamarDiaria(Player jugador) {
        return reclamarDiaria(jugador, false);
    }

    public boolean reclamarDiaria(Player jugador, boolean silencioso) {
        if (!empezarReclamo(jugador)) {
            return false;
        }
        try {
            return reclamarDiariaInterno(jugador, silencioso);
        } finally {
            terminarReclamo(jugador);
        }
    }

    private boolean reclamarDiariaInterno(Player jugador, boolean silencioso) {
        if (!plugin.ajustes().moduloDiaria()) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.modulo-desactivado", Mensajes.ph("modulo", "diaria"));
            }
            return false;
        }
        if (!jugador.hasPermission("jprewards.claim.daily")) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.sin-permiso");
                plugin.sonidos().error(jugador);
            }
            return false;
        }
        DatosJugador datos = datos(jugador);
        long claveHoy = Tiempo.claveDia(plugin.ajustes().horaReinicio());
        if (!puedeReclamarDiaria(datos)) {
            if (!silencioso) {
                if (puedeAvisar(jugador, "diaria.ya-reclamada")) {
                    plugin.mensajes().enviar(jugador, "diaria.ya-reclamada", Mensajes.ph("tiempo", tiempoRestante()));
                }
                plugin.sonidos().error(jugador);
            }
            return false;
        }

        int dia = proximoDia(datos);
        Recompensa recompensa = ciclo.get(Integer.valueOf(dia));
        if (recompensa == null) {
            plugin.getLogger().warning("No reward defined for day " + dia + " of the cycle.");
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.error");
                plugin.sonidos().error(jugador);
            }
            return false;
        }
        if (recompensa.requierePermiso() && !jugador.hasPermission(recompensa.permiso())) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "recompensa.sin-permiso-entrada",
                        Mensajes.ph("recompensa", recompensa.nombrePara(jugador)));
                plugin.sonidos().error(jugador);
            }
            return false;
        }

        Verdict verdicto = condiciones(jugador, recompensa, datos);
        if (verdicto == Verdict.BLOQUEAR) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.mundo-bloqueado",
                        Mensajes.ph("mundo", recompensa.mundo()));
                plugin.sonidos().error(jugador);
            }
            return false;
        }

        // Racha y bonus
        boolean consecutivo = datos.ultimoDia() > 0 && (claveHoy - datos.ultimoDia()) == 1;
        if (consecutivo || !plugin.ajustes().perderRachaAlFaltar()) {
            datos.racha(datos.racha() + 1);
        } else {
            if (datos.racha() >= 2) {
                plugin.mensajes().enviar(jugador, "diaria.racha-perdida",
                        Mensajes.ph("racha", String.valueOf(datos.racha())));
            }
            datos.racha(1);
            datos.reiniciarBonus();
        }
        if (datos.racha() > datos.mejorRacha()) {
            datos.mejorRacha(datos.racha());
        }

        datos.ultimoDia(claveHoy);
        datos.diasMes(datos.diasMes() + 1);
        datos.total(datos.total() + 1);
        datos.marcarReclamo(System.currentTimeMillis());

        String nombre = recompensa.nombrePara(jugador);
        boolean entregado = entregarSiProcede(jugador, recompensa, nombre, verdicto, datos, silencioso);
        plugin.datos().guardar(datos);

        if (!silencioso && entregado) {
            plugin.sonidos().exito(jugador);
            plugin.mensajes().enviar(jugador, "diaria.reclamada", Mensajes.ph("dia", String.valueOf(dia), "racha",
                    String.valueOf(datos.racha()), "premio", nombre));
        }
        return true;
    }

    /** Reclama la recompensa semanal. Devuelve true si se entrego. */
    public boolean reclamarSemanal(Player jugador) {
        return reclamarSemanal(jugador, false);
    }

    public boolean reclamarSemanal(Player jugador, boolean silencioso) {
        if (!empezarReclamo(jugador)) {
            return false;
        }
        try {
            return reclamarSemanalInterno(jugador, silencioso);
        } finally {
            terminarReclamo(jugador);
        }
    }

    private boolean reclamarSemanalInterno(Player jugador, boolean silencioso) {
        if (!plugin.ajustes().moduloSemanal()) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.modulo-desactivado", Mensajes.ph("modulo", "semanal"));
            }
            return false;
        }
        if (!jugador.hasPermission("jprewards.claim.weekly")) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.sin-permiso");
                plugin.sonidos().error(jugador);
            }
            return false;
        }
        DatosJugador datos = datos(jugador);
        long claveAhora = Tiempo.claveSemana(plugin.ajustes().horaReinicio(), plugin.ajustes().diaInicioSemana());
        if (!puedeReclamarSemanal(datos)) {
            if (!silencioso) {
                if (puedeAvisar(jugador, "semanal.ya-reclamada")) {
                    plugin.mensajes().enviar(jugador, "semanal.ya-reclamada",
                            Mensajes.ph("tiempo_semanal", tiempoRestanteSemanal()));
                }
                plugin.sonidos().error(jugador);
            }
            return false;
        }

        int semana = proximaSemana(datos);
        Recompensa recompensa = cicloSemanal.get(Integer.valueOf(semana));
        if (recompensa == null) {
            plugin.getLogger().warning("No reward defined for week " + semana + " of the cycle.");
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.error");
                plugin.sonidos().error(jugador);
            }
            return false;
        }
        if (recompensa.requierePermiso() && !jugador.hasPermission(recompensa.permiso())) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "recompensa.sin-permiso-entrada",
                        Mensajes.ph("recompensa", recompensa.nombrePara(jugador)));
                plugin.sonidos().error(jugador);
            }
            return false;
        }

        Verdict verdictoSemanal = condiciones(jugador, recompensa, datos);
        if (verdictoSemanal == Verdict.BLOQUEAR) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.mundo-bloqueado",
                        Mensajes.ph("mundo", recompensa.mundo()));
                plugin.sonidos().error(jugador);
            }
            return false;
        }

        boolean consecutiva = datos.ultimaSemana() > 0 && (claveAhora - datos.ultimaSemana()) == 1;
        if (consecutiva || !plugin.ajustes().perderRachaSemanal()) {
            datos.rachaSemanal(datos.rachaSemanal() + 1);
        } else {
            if (datos.rachaSemanal() >= 2) {
                plugin.mensajes().enviar(jugador, "semanal.racha-perdida",
                        Mensajes.ph("racha_semanal", String.valueOf(datos.rachaSemanal())));
            }
            datos.rachaSemanal(1);
            reiniciarBonusSemanales(datos);
        }
        if (datos.rachaSemanal() > datos.mejorRachaSemanal()) {
            datos.mejorRachaSemanal(datos.rachaSemanal());
        }

        datos.ultimaSemana(claveAhora);
        datos.totalSemanal(datos.totalSemanal() + 1);
        datos.marcarReclamo(System.currentTimeMillis());

        String nombre = recompensa.nombrePara(jugador);
        boolean entregadoSemanal = entregarSiProcede(jugador, recompensa, nombre, verdictoSemanal, datos, silencioso);
        plugin.datos().guardar(datos);

        if (!silencioso && entregadoSemanal) {
            plugin.sonidos().exito(jugador);
            plugin.mensajes().enviar(jugador, "semanal.reclamada", Mensajes.ph("semana", String.valueOf(semana),
                    "racha_semanal", String.valueOf(datos.rachaSemanal()), "premio", nombre));
        }
        return true;
    }

    /** Al perder la racha semanal, sus bonus vuelven a estar disponibles. */
    private void reiniciarBonusSemanales(DatosJugador datos) {
        for (BonusRacha bono : bonus) {
            if (bono.tipo() == BonusRacha.Tipo.SEMANAL) {
                datos.bonusReclamados().remove(bono.id());
            }
        }
        datos.marcarSucio();
    }

    /** Reclama un hito mensual. Devuelve true si se entrego. */
    public boolean reclamarHito(Player jugador, Hito hito) {
        return reclamarHito(jugador, hito, false);
    }

    public boolean reclamarHito(Player jugador, Hito hito, boolean silencioso) {
        if (!empezarReclamo(jugador)) {
            return false;
        }
        try {
            return reclamarHitoInterno(jugador, hito, silencioso);
        } finally {
            terminarReclamo(jugador);
        }
    }

    private boolean reclamarHitoInterno(Player jugador, Hito hito, boolean silencioso) {
        if (hito == null) {
            return false;
        }
        if (!plugin.ajustes().moduloMensual()) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.modulo-desactivado", Mensajes.ph("modulo", "mensual"));
            }
            return false;
        }
        if (!jugador.hasPermission("jprewards.claim.monthly")) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.sin-permiso");
                plugin.sonidos().error(jugador);
            }
            return false;
        }
        DatosJugador datos = datos(jugador);
        if (datos.hitosReclamados().contains(hito.id())) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "mensual.hito-ya-reclamado",
                        Mensajes.ph("dias", String.valueOf(hito.diasRequeridos())));
                plugin.sonidos().error(jugador);
            }
            return false;
        }
        if (datos.diasMes() < hito.diasRequeridos()) {
            if (!silencioso) {
                if (puedeAvisar(jugador, "mensual.hito-bloqueado")) {
                    plugin.mensajes().enviar(jugador, "mensual.hito-bloqueado", Mensajes.ph("faltan",
                            String.valueOf(hito.diasRequeridos() - datos.diasMes())));
                }
                plugin.sonidos().error(jugador);
            }
            return false;
        }
        if (hito.recompensa().requierePermiso() && !jugador.hasPermission(hito.recompensa().permiso())) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "recompensa.sin-permiso-entrada",
                        Mensajes.ph("recompensa", hito.recompensa().nombrePara(jugador)));
                plugin.sonidos().error(jugador);
            }
            return false;
        }

        Verdict verdictoHito = condiciones(jugador, hito.recompensa(), datos);
        if (verdictoHito == Verdict.BLOQUEAR) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.mundo-bloqueado",
                        Mensajes.ph("mundo", hito.recompensa().mundo()));
                plugin.sonidos().error(jugador);
            }
            return false;
        }

        datos.hitosReclamados().add(hito.id());
        datos.hitosHistoricos(datos.hitosHistoricos() + 1);
        datos.marcarReclamo(System.currentTimeMillis());

        String nombre = hito.recompensa().nombrePara(jugador);
        boolean entregadoHito = entregarSiProcede(jugador, hito.recompensa(), nombre, verdictoHito, datos, silencioso);
        plugin.datos().guardar(datos);

        if (!silencioso && entregadoHito) {
            plugin.sonidos().exito(jugador);
            plugin.mensajes().enviar(jugador, "mensual.hito-reclamado", Mensajes.ph("dias",
                    String.valueOf(hito.diasRequeridos()), "premio", nombre));
        }
        return true;
    }

    /** Reclama un bonus de racha. Devuelve true si se entrego. */
    public boolean reclamarBonus(Player jugador, BonusRacha bono) {
        return reclamarBonus(jugador, bono, false);
    }

    public boolean reclamarBonus(Player jugador, BonusRacha bono, boolean silencioso) {
        if (!empezarReclamo(jugador)) {
            return false;
        }
        try {
            return reclamarBonusInterno(jugador, bono, silencioso);
        } finally {
            terminarReclamo(jugador);
        }
    }

    private boolean reclamarBonusInterno(Player jugador, BonusRacha bono, boolean silencioso) {
        if (bono == null) {
            return false;
        }
        if (!plugin.ajustes().moduloRachas()) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.modulo-desactivado", Mensajes.ph("modulo", "rachas"));
            }
            return false;
        }
        if (!jugador.hasPermission("jprewards.claim.streak")) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.sin-permiso");
                plugin.sonidos().error(jugador);
            }
            return false;
        }
        DatosJugador datos = datos(jugador);
        if (datos.bonusReclamados().contains(bono.id())) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "rachas.bonus-ya-reclamado",
                        Mensajes.ph("racha_minima", String.valueOf(bono.rachaMinima())));
                plugin.sonidos().error(jugador);
            }
            return false;
        }
        int racha = rachaDe(datos, bono);
        if (racha < bono.rachaMinima()) {
            if (!silencioso) {
                if (puedeAvisar(jugador, "rachas.bonus-bloqueado")) {
                    plugin.mensajes().enviar(jugador, "rachas.bonus-bloqueado", Mensajes.ph("racha_minima",
                            String.valueOf(bono.rachaMinima()), "racha", String.valueOf(racha)));
                }
                plugin.sonidos().error(jugador);
            }
            return false;
        }
        if (bono.recompensa().requierePermiso() && !jugador.hasPermission(bono.recompensa().permiso())) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "recompensa.sin-permiso-entrada",
                        Mensajes.ph("recompensa", bono.recompensa().nombrePara(jugador)));
                plugin.sonidos().error(jugador);
            }
            return false;
        }

        Verdict verdictoBonus = condiciones(jugador, bono.recompensa(), datos);
        if (verdictoBonus == Verdict.BLOQUEAR) {
            if (!silencioso) {
                plugin.mensajes().enviar(jugador, "general.mundo-bloqueado",
                        Mensajes.ph("mundo", bono.recompensa().mundo()));
                plugin.sonidos().error(jugador);
            }
            return false;
        }

        datos.bonusReclamados().add(bono.id());
        datos.rachaBonusClave(racha);
        datos.marcarReclamo(System.currentTimeMillis());

        String nombre = bono.recompensa().nombrePara(jugador);
        boolean entregadoBonus = entregarSiProcede(jugador, bono.recompensa(), nombre, verdictoBonus, datos, silencioso);
        plugin.datos().guardar(datos);

        if (!silencioso && entregadoBonus) {
            plugin.sonidos().exito(jugador);
            plugin.mensajes().enviar(jugador, "rachas.bonus-reclamado",
                    Mensajes.ph("racha_minima", String.valueOf(bono.rachaMinima()), "premio", nombre));
        }
        return true;
    }

    /**
     * Reclama TODO lo que el jugador tenga disponible (diaria, semanal, hitos
     * y bonus). Devuelve cuantas cosas se entregaron.
     */
    public int reclamarTodo(Player jugador) {
        DatosJugador datos = datos(jugador);
        int entregadas = 0;
        if (puedeReclamarDiaria(datos) && reclamarDiaria(jugador, true)) {
            entregadas++;
        }
        if (puedeReclamarSemanal(datos) && reclamarSemanal(jugador, true)) {
            entregadas++;
        }
        for (Hito hito : hitosDisponibles(datos)) {
            if (reclamarHito(jugador, hito, true)) {
                entregadas++;
            }
        }
        for (BonusRacha bono : bonusDisponibles(datos)) {
            if (reclamarBonus(jugador, bono, true)) {
                entregadas++;
            }
        }
        if (entregadas > 0) {
            plugin.sonidos().exito(jugador);
            plugin.mensajes().enviar(jugador, "general.reclamar-todo",
                    Mensajes.ph("cantidad", String.valueOf(entregadas)));
        } else {
            if (puedeAvisar(jugador, "general.nada-para-reclamar")) {
                plugin.mensajes().enviar(jugador, "general.nada-para-reclamar");
            }
            plugin.sonidos().error(jugador);
        }
        return entregadas;
    }

    /**
     * Entrega una recompensa concreta SIN tocar contadores ni esperas: es la
     * herramienta de administracion (/rewards give) para probar premios.
     */
    public boolean regalar(Player jugador, String tipo, String clave) {
        Recompensa recompensa = buscar(tipo, clave);
        if (recompensa == null) {
            return false;
        }
        String nombre = recompensa.nombrePara(jugador);
        entrega.entregar(jugador, recompensa, nombre);
        plugin.sonidos().exito(jugador);
        return true;
    }

    /** Busca una recompensa por tipo y clave (dia, semana, id de hito o bonus). */
    public Recompensa buscar(String tipo, String clave) {
        String limpio = clave == null ? "" : clave.trim();
        try {
            if ("diaria".equals(tipo)) {
                int dia = limpio.isEmpty() ? 1 : Integer.parseInt(limpio);
                return ciclo.get(Integer.valueOf(dia));
            }
            if ("semanal".equals(tipo)) {
                int semana = limpio.isEmpty() ? 1 : Integer.parseInt(limpio);
                return cicloSemanal.get(Integer.valueOf(semana));
            }
        } catch (NumberFormatException error) {
            return null;
        }
        if ("mensual".equals(tipo)) {
            Hito hito = limpio.isEmpty() ? (hitos.isEmpty() ? null : hitos.get(0)) : hito(limpio);
            return hito == null ? null : hito.recompensa();
        }
        if ("rachas".equals(tipo)) {
            BonusRacha bono = limpio.isEmpty() ? (bonus.isEmpty() ? null : bonus.get(0)) : bonus(limpio);
            return bono == null ? null : bono.recompensa();
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Avisos
    // ------------------------------------------------------------------

    /** Cuantas cosas puede reclamar ahora mismo. */
    public int disponibles(Player jugador) {
        DatosJugador datos = datos(jugador);
        int total = 0;
        if (puedeReclamarDiaria(datos)) {
            total++;
        }
        if (puedeReclamarSemanal(datos)) {
            total++;
        }
        total += hitosDisponibles(datos).size();
        total += bonusDisponibles(datos).size();
        return total;
    }

    /** Aviso al entrar: diaria, semanal, hitos y bonus disponibles. */
    public void avisarDisponibles(Player jugador) {
        if (!jugador.hasPermission("jprewards.notify")) {
            return;
        }
        DatosJugador datos = datos(jugador);
        boolean alguno = false;

        if (plugin.ajustes().avisoDiaria() && puedeReclamarDiaria(datos)) {
            enviarAviso(jugador, "avisos.diaria");
            alguno = true;
        }
        if (plugin.ajustes().avisoSemanal() && puedeReclamarSemanal(datos)) {
            enviarAviso(jugador, "avisos.semanal");
            alguno = true;
        }
        if (plugin.ajustes().avisoHitos()) {
            int pendientes = hitosDisponibles(datos).size();
            if (pendientes > 0) {
                enviarAviso(jugador, "avisos.hitos", Mensajes.ph("cantidad", String.valueOf(pendientes)));
                alguno = true;
            }
        }
        if (plugin.ajustes().avisoRachas()) {
            int pendientes = bonusDisponibles(datos).size();
            if (pendientes > 0) {
                enviarAviso(jugador, "avisos.rachas", Mensajes.ph("cantidad", String.valueOf(pendientes)));
                alguno = true;
            }
        }
        if (alguno) {
            plugin.sonidos().aviso(jugador);
        }
    }

    /** Envia un aviso por action bar (con degradacion a chat) o por chat. */
    private void enviarAviso(Player jugador, String clave) {
        enviarAviso(jugador, clave, null);
    }

    private void enviarAviso(Player jugador, String clave, Map<String, String> placeholders) {
        if ("chat".equals(plugin.ajustes().avisoTipo())) {
            plugin.mensajes().enviar(jugador, clave, placeholders);
            return;
        }
        String texto = plugin.mensajes().texto(clave, placeholders, jugador);
        if (texto.isEmpty()) {
            return;
        }
        if (!Compat.actionBar(jugador, texto)) {
            plugin.mensajes().enviar(jugador, clave, placeholders);
        }
    }

    /** Nombre visible de la recompensa para titulos y mensajes. */
    public String nombreCorto(Recompensa recompensa, Player jugador, String porDefecto) {
        if (recompensa == null) {
            return porDefecto;
        }
        String nombre = recompensa.nombrePara(jugador);
        return nombre == null || nombre.isEmpty() ? porDefecto : Texto.plano(nombre);
    }

    /** Entrega los premios que no cabian en el inventario. */
    public void entregarPendientes(Player jugador) {
        entrega.entregarPendientes(jugador);
    }
}
