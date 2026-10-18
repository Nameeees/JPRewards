package me.jp.jprewards.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Datos persistentes de un jugador.
 *
 * Se guardan en YAML (un archivo por jugador o uno solo) o en una base de
 * datos, siempre con el mismo formato logico, asi que se puede migrar de un
 * backend a otro sin perder nada.
 *
 * Campos:
 *   ultimoDia / racha / mejorRacha / total          -> ciclo diario
 *   ultimaSemana / rachaSemanal / ... / totalSemanal -> ciclo semanal
 *   mesClave / diasMes / hitosReclamados             -> mes actual
 *   hitosHistoricos                                   -> hitos de toda la vida
 *   bonusReclamados / rachaBonusClave                 -> bonus de racha
 *   pendientes                                        -> items sin entregar
 */
public final class DatosJugador {

    private final UUID uuid;
    private String nombre = "";

    private long ultimoDia;
    private int racha;
    private int mejorRacha;
    private int total;

    private long ultimaSemana;
    private int rachaSemanal;
    private int mejorRachaSemanal;
    private int totalSemanal;

    private int mesClave;
    private int diasMes;
    private final Set<String> hitosReclamados = new LinkedHashSet<String>();
    private int hitosHistoricos;

    private final Set<String> bonusReclamados = new LinkedHashSet<String>();
    private int rachaBonusClave;

    private long primerReclamo;
    private long ultimoReclamo;

    private final List<ItemStack> pendientes = new ArrayList<ItemStack>();

    private boolean sucio;

    public DatosJugador(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public String nombre() {
        return nombre;
    }

    public void nombre(String valor) {
        String limpio = valor == null ? "" : valor;
        if (!limpio.equals(nombre)) {
            nombre = limpio;
            sucio = true;
        }
    }

    // ---- Ciclo diario ------------------------------------------------

    public long ultimoDia() {
        return ultimoDia;
    }

    public void ultimoDia(long valor) {
        ultimoDia = valor;
        sucio = true;
    }

    public int racha() {
        return racha;
    }

    public void racha(int valor) {
        racha = Math.max(0, valor);
        sucio = true;
    }

    public int mejorRacha() {
        return mejorRacha;
    }

    public void mejorRacha(int valor) {
        mejorRacha = Math.max(0, valor);
        sucio = true;
    }

    public int total() {
        return total;
    }

    public void total(int valor) {
        total = Math.max(0, valor);
        sucio = true;
    }

    // ---- Ciclo semanal -----------------------------------------------

    public long ultimaSemana() {
        return ultimaSemana;
    }

    public void ultimaSemana(long valor) {
        ultimaSemana = valor;
        sucio = true;
    }

    public int rachaSemanal() {
        return rachaSemanal;
    }

    public void rachaSemanal(int valor) {
        rachaSemanal = Math.max(0, valor);
        sucio = true;
    }

    public int mejorRachaSemanal() {
        return mejorRachaSemanal;
    }

    public void mejorRachaSemanal(int valor) {
        mejorRachaSemanal = Math.max(0, valor);
        sucio = true;
    }

    public int totalSemanal() {
        return totalSemanal;
    }

    public void totalSemanal(int valor) {
        totalSemanal = Math.max(0, valor);
        sucio = true;
    }

    // ---- Mes ---------------------------------------------------------

    public int mesClave() {
        return mesClave;
    }

    public int diasMes() {
        return diasMes;
    }

    public void diasMes(int valor) {
        diasMes = Math.max(0, valor);
        sucio = true;
    }

    public Set<String> hitosReclamados() {
        return hitosReclamados;
    }

    public int hitosHistoricos() {
        return hitosHistoricos;
    }

    public void hitosHistoricos(int valor) {
        hitosHistoricos = Math.max(0, valor);
        sucio = true;
    }

    // ---- Bonus de racha ----------------------------------------------

    public Set<String> bonusReclamados() {
        return bonusReclamados;
    }

    public int rachaBonusClave() {
        return rachaBonusClave;
    }

    public void rachaBonusClave(int valor) {
        rachaBonusClave = Math.max(0, valor);
        sucio = true;
    }

    // ---- Fechas y pendientes -----------------------------------------

    public long primerReclamo() {
        return primerReclamo;
    }

    /** Marca la fecha del reclamo (solo la primera vez guarda 'primerReclamo'). */
    public void marcarReclamo(long milisegundos) {
        if (primerReclamo <= 0) {
            primerReclamo = milisegundos;
        }
        ultimoReclamo = milisegundos;
        sucio = true;
    }

    public long ultimoReclamo() {
        return ultimoReclamo;
    }

    public List<ItemStack> pendientes() {
        return pendientes;
    }

    public boolean tienePendientes() {
        return !pendientes.isEmpty();
    }

    public void agregarPendiente(ItemStack item) {
        if (item != null) {
            pendientes.add(item.clone());
            sucio = true;
        }
    }

    /** Devuelve copia de los pendientes y los vacia. */
    public List<ItemStack> tomarPendientes() {
        List<ItemStack> copia = new ArrayList<ItemStack>(pendientes.size());
        for (ItemStack item : pendientes) {
            copia.add(item.clone());
        }
        if (!pendientes.isEmpty()) {
            pendientes.clear();
            sucio = true;
        }
        return copia;
    }

    // ---- Estado ------------------------------------------------------

    public boolean sucio() {
        return sucio;
    }

    public void marcarLimpio() {
        sucio = false;
    }

    public void marcarSucio() {
        sucio = true;
    }

    /**
     * Ajusta el mes actual: si el jugador viene de otro mes se reinicia el
     * progreso mensual (dias e hitos), conservando lo historico.
     */
    public void asegurarMes(int nuevoMesClave) {
        if (mesClave != nuevoMesClave) {
            mesClave = nuevoMesClave;
            diasMes = 0;
            hitosReclamados.clear();
            sucio = true;
        }
    }

    /**
     * Reinicio aplicado a la racha: los bonus vuelven a estar disponibles en
     * la nueva racha (se reclaman una vez por racha).
     */
    public void reiniciarBonus() {
        if (!bonusReclamados.isEmpty()) {
            bonusReclamados.clear();
            sucio = true;
        }
        rachaBonusClave = 0;
    }

    /**
     * Reinicio administrativo.
     *
     * @param tipo diaria | semanal | mensual | rachas | todo
     */
    public void reiniciar(String tipo) {
        if ("diaria".equals(tipo) || "todo".equals(tipo)) {
            ultimoDia = 0L;
            racha = 0;
            // mejorRacha se conserva: es historico.
        }
        if ("semanal".equals(tipo) || "todo".equals(tipo)) {
            ultimaSemana = 0L;
            rachaSemanal = 0;
        }
        if ("mensual".equals(tipo) || "todo".equals(tipo)) {
            mesClave = 0;
            diasMes = 0;
            hitosReclamados.clear();
        }
        if ("rachas".equals(tipo) || "todo".equals(tipo)) {
            bonusReclamados.clear();
            rachaBonusClave = 0;
        }
        if ("todo".equals(tipo)) {
            total = 0;
            mejorRacha = 0;
            totalSemanal = 0;
            mejorRachaSemanal = 0;
            hitosHistoricos = 0;
            primerReclamo = 0L;
            ultimoReclamo = 0L;
            pendientes.clear();
        }
        sucio = true;
    }

    // ---- Persistencia (usado por DatosCodec) -------------------------

    public void cargarEstado(String nombre, long ultimoDia, int racha, int mejorRacha, int total, long ultimaSemana,
            int rachaSemanal, int mejorRachaSemanal, int totalSemanal, int mesClave, int diasMes, Set<String> hitos,
            int hitosHistoricos, Set<String> bonus, int rachaBonusClave, long primerReclamo, long ultimoReclamo) {
        this.nombre = nombre == null ? "" : nombre;
        this.ultimoDia = ultimoDia;
        this.racha = racha;
        this.mejorRacha = mejorRacha;
        this.total = total;
        this.ultimaSemana = ultimaSemana;
        this.rachaSemanal = rachaSemanal;
        this.mejorRachaSemanal = mejorRachaSemanal;
        this.totalSemanal = totalSemanal;
        this.mesClave = mesClave;
        this.diasMes = diasMes;
        this.hitosReclamados.clear();
        if (hitos != null) {
            this.hitosReclamados.addAll(hitos);
        }
        this.hitosHistoricos = hitosHistoricos;
        this.bonusReclamados.clear();
        if (bonus != null) {
            this.bonusReclamados.addAll(bonus);
        }
        this.rachaBonusClave = rachaBonusClave;
        this.primerReclamo = primerReclamo;
        this.ultimoReclamo = ultimoReclamo;
        sucio = false;
    }

    public void cargarPendientes(List<ItemStack> items) {
        pendientes.clear();
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null) {
                    pendientes.add(item);
                }
            }
        }
    }
}
