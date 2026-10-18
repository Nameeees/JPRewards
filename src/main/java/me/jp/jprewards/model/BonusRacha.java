package me.jp.jprewards.model;

/**
 * Bonus por racha: se desbloquea al alcanzar N dias (o semanas) seguidos y
 * se puede reclamar UNA vez por racha. Cuando la racha se pierde y vuelve a
 * empezar, los bonus quedan disponibles otra vez.
 *
 * Se definen en recompensas/rachas.yml.
 */
public final class BonusRacha {

    /** Sobre que racha se calcula el bonus. */
    public enum Tipo {
        DIARIA("diaria"),
        SEMANAL("semanal");

        private final String clave;

        Tipo(String clave) {
            this.clave = clave;
        }

        public String clave() {
            return clave;
        }

        /** Devuelve null si el texto no es un tipo valido. */
        public static Tipo desde(String texto) {
            if (texto == null) {
                return null;
            }
            String limpio = texto.trim().toLowerCase(java.util.Locale.ROOT);
            for (Tipo tipo : values()) {
                if (tipo.clave.equals(limpio)) {
                    return tipo;
                }
            }
            return null;
        }
    }

    private final String id;
    private final Tipo tipo;
    private final int rachaMinima;
    private final Recompensa recompensa;

    public BonusRacha(String id, Tipo tipo, int rachaMinima, Recompensa recompensa) {
        this.id = id;
        this.tipo = tipo == null ? Tipo.DIARIA : tipo;
        this.rachaMinima = Math.max(1, rachaMinima);
        this.recompensa = recompensa;
    }

    public String id() {
        return id;
    }

    public Tipo tipo() {
        return tipo;
    }

    public int rachaMinima() {
        return rachaMinima;
    }

    public Recompensa recompensa() {
        return recompensa;
    }

    /** Nombre visible por defecto: "Racha de 7 dias". */
    public String nombrePorDefecto() {
        return tipo == Tipo.SEMANAL ? rachaMinima + " week streak" : rachaMinima + " day streak";
    }
}
