package me.jp.jprewards.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Se dispara justo ANTES de entregar una recompensa (la de un ciclo o un premio
 * suelto). Es cancelable: si la cancelas, no se entrega nada.
 *
 * <pre>
 * &#64;EventHandler(priority = EventPriority.HIGH)
 * public void alEntregar(RewardClaimEvent evento) {
 *     if (evento.getClave().equals("7") &amp;&amp; evento.getPlayer().isFlying()) {
 *         evento.setCancelled(true);
 *     }
 * }
 * </pre>
 */
public class RewardClaimEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String id;
    private final String tipo;
    private final String clave;
    private final String nombre;
    private boolean cancelled;

    public RewardClaimEvent(Player player, String id, String tipo, String clave, String nombre) {
        this.player = player;
        this.id = id == null ? "" : id;
        this.tipo = tipo == null ? "" : tipo;
        this.clave = clave == null ? "" : clave;
        this.nombre = nombre == null ? "" : nombre;
    }

    /** Jugador que va a recibir la recompensa. */
    public Player getPlayer() {
        return player;
    }

    /** Id interno de la recompensa (por ejemplo 'dia-3' o 'hito-hito-15'). */
    public String getId() {
        return id;
    }

    /** Ciclo: daily, weekly, monthly, streaks o extra. */
    public String getTipo() {
        return tipo;
    }

    /** Clave dentro del ciclo: el dia, la semana, el id del hito o el del premio suelto. */
    public String getClave() {
        return clave;
    }

    /** Nombre visible de la recompensa. */
    public String getNombre() {
        return nombre;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelar) {
        this.cancelled = cancelar;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
