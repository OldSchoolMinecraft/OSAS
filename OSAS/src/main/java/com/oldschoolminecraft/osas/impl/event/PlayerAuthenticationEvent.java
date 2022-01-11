package com.oldschoolminecraft.osas.impl.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.UUID;

public class PlayerAuthenticationEvent extends Event {
    private UUID player;
    private boolean authenticated;

    public PlayerAuthenticationEvent(UUID player, boolean authenticated) {
        super("PlayerAuthenticationEvent");
        this.player = player;
        this.authenticated = authenticated;
    }

    public UUID getPlayer() {
        return player;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
