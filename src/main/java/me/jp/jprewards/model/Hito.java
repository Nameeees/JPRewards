package me.jp.jprewards.model;

/**
 * Hito mensual: se desbloquea al reclamar N dias de la diaria dentro del mes
 * actual (se define en recompensas/mensuales.yml).
 */
public final class Hito {

    private final String id;
    private final int diasRequeridos;
    private final Recompensa recompensa;

    public Hito(String id, int diasRequeridos, Recompensa recompensa) {
        this.id = id;
        this.diasRequeridos = Math.max(1, diasRequeridos);
        this.recompensa = recompensa;
    }

    public String id() {
        return id;
    }

    public int diasRequeridos() {
        return diasRequeridos;
    }

    public Recompensa recompensa() {
        return recompensa;
    }

    /** Nombre visible por defecto: "Hito de 15 dias". */
    public String nombrePorDefecto() {
        return "Hito de " + diasRequeridos + " dias";
    }
}
