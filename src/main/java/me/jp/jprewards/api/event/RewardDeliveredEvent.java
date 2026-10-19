package me.jp.jprewards.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Se dispara DESPUES de entregar una recompensa, con el resumen de lo entregado.
 * No es cancelable.
 */
public class RewardDeliveredEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String id;
    private final String tipo;
    private final String clave;
    private final String nombre;
    private final int comandos;
    private final int items;
    private final int experiencia;

    public RewardDeliveredEvent(Player player, String id, String tipo, String clave, String nombre,
            int comandos, int items, int experiencia) {
        this.player = player;
        this.id = id == null ? "" : id;
        this.tipo = tipo == null ? "" : tipo;
        this.clave = clave == null ? "" : clave;
        this.nombre = nombre == null ? "" : nombre;
        this.comandos = comandos;
        this.items = items;
        this.experiencia = experiencia;
    }

    public Player getPlayer() {
        return player;
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getClave() {
        return clave;
    }

    public String getNombre() {
        return nombre;
    }

    public int getComandos() {
        return comandos;
    }

    public int getItems() {
        return items;
    }

    public int getExperiencia() {
        return experiencia;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
