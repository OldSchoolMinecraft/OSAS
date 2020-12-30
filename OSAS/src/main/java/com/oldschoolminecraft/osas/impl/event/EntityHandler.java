package com.oldschoolminecraft.osas.impl.event;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityTargetEvent;

import com.oldschoolminecraft.osas.OSAS;
import com.oldschoolminecraft.osas.impl.fallback.FallbackManager;

public class EntityHandler extends EntityListener
{
    private OSAS osas = OSAS.instance;
    private FallbackManager fm = osas.fallbackManager;
    
    public void onEntityDamage(final EntityDamageEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            final Player player = (Player) event.getEntity();
            if (!fm.isAuthenticated(player.getName().toLowerCase()) || fm.isFrozen(player.getName().toLowerCase()))
                event.setCancelled(true);
        }
    }
    
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            final Player player = (Player) event.getEntity();
            if (!fm.isAuthenticated(player.getName().toLowerCase()) || fm.isFrozen(player.getName().toLowerCase()))
                event.setCancelled(true);
        }
    }
    
    public void onEntityTarget(final EntityTargetEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            final Player player = (Player) event.getEntity();
            if (!fm.isAuthenticated(player.getName().toLowerCase()) || fm.isFrozen(player.getName().toLowerCase()))
                event.setCancelled(true);
        }
    }
}
