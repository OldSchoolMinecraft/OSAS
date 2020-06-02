package com.oldschoolminecraft.osas.impl.event;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import com.oldschoolminecraft.osas.OSAS;
import com.oldschoolminecraft.osas.Util;
import com.oldschoolminecraft.osas.impl.Hook;
import com.oldschoolminecraft.osas.impl.fallback.FallbackManager;
import com.projectposeidon.johnymuffin.ConnectionPause;

@SuppressWarnings("all")
public class PlayerHandler extends PlayerListener
{
    private OSAS osas = OSAS.instance;
    private FallbackManager fm = osas.fallbackManager;

    public void onPlayerPreLogin(PlayerPreLoginEvent event)
    {
        String username = event.getName();
        String ip = event.getAddress().getHostAddress();

        ConnectionPause pause = event.addConnectionPause(osas, "OSAS");
        Bukkit.getScheduler().scheduleAsyncDelayedTask(osas, () ->
        {
            try
            {
                boolean authenticated = false;
                for (String hook_name : osas.manager.getHooksMap().keySet())
                {
                    Hook hook = osas.manager.getHooksMap().get(hook_name);
                    boolean result = hook.authenticate(username, ip);
                    System.out.println(String.format("OSAS/%s: Authentication result for '%s': %s", hook_name, username, result));
                    if (result)
                        authenticated = true;
                }

                if (authenticated)
                {
                    event.allow();
                    
                    fm.authenticatePlayer(username);
                    if (fm.isRegistered(username))
                    {
                        if (!fm.isApproved(username))
                            fm.freezePlayer(username);
                    } else if (osas.dc.hasLegacyData(username)) {
                        fm.freezePlayer(username);
                    } else
                        fm.freezePlayer(username);
                } else {
                    // fallback authentication
                    scheduleAfterJoin(username);
                }

                event.removeConnectionPause(pause);
            } catch (IOException ex) {
                event.removeConnectionPause(pause);
                ex.printStackTrace();
            }
        });
    }

    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        try
        {
            fm.deauthenticatePlayer(event.getPlayer().getName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onPlayerMove(final PlayerMoveEvent event)
    {
        final Player player = event.getPlayer();
        final Location fromLoc = event.getFrom();
        final Location toLoc = event.getTo();
        if (fromLoc.getX() == toLoc.getX() && fromLoc.getZ() == toLoc.getZ() && fromLoc.getY() > toLoc.getY())
            return;
        if (!fm.isAuthenticated(player.getName()) || fm.isFrozen(player.getName()))
        {
            this.warn(event.getPlayer());
            event.setCancelled(true);
            player.teleport(fromLoc);
        }
    }

    public void onPlayerDropItem(final PlayerDropItemEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName()) || fm.isFrozen(event.getPlayer().getName()))
            event.setCancelled(true);
    }

    public void onPlayerPickupItem(final PlayerPickupItemEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName()) || fm.isFrozen(event.getPlayer().getName()))
            event.setCancelled(true);
    }

    public void onPlayerBedEnter(final PlayerBedEnterEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName()) || fm.isFrozen(event.getPlayer().getName()))
            event.setCancelled(true);
    }

    public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName()) || fm.isFrozen(event.getPlayer().getName()))
            event.setCancelled(true);
    }

    public void onPlayerBucketFill(final PlayerBucketFillEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName()) || fm.isFrozen(event.getPlayer().getName()))
            event.setCancelled(true);
    }

    public void onPlayerChat(final PlayerChatEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName()) || fm.isFrozen(event.getPlayer().getName()))
        {
            event.setMessage("");
            event.setCancelled(true);
            this.warn(event.getPlayer());
        }
    }

    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName()) || fm.isFrozen(event.getPlayer().getName()))
        {
            final String label = event.getMessage().split(" ")[0];
            if (label.equalsIgnoreCase("/register"))
                return;
            if (label.equalsIgnoreCase("/login"))
                return;
            event.setMessage("/help");
            event.setCancelled(true);
            this.warn(event.getPlayer());
        }
    }

    public void onPlayerInteract(final PlayerInteractEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName()) || fm.isFrozen(event.getPlayer().getName()))
            event.setCancelled(true);
    }

    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName()) || fm.isFrozen(event.getPlayer().getName()))
            event.setCancelled(true);
    }

    public void onPlayerPortal(final PlayerPortalEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName()) || fm.isFrozen(event.getPlayer().getName()))
            event.setCancelled(true);
    }

    private void scheduleAfterJoin(String username)
    {
        Bukkit.getScheduler().scheduleAsyncDelayedTask((Plugin) OSAS.instance, (Runnable) new Runnable()
        {
            @Override
            public void run()
            {
                fm.handleFallbackForPlayer(username);
            }
        }, 2L);
    }

    private void warn(Player player)
    {
        if (fm.isRegistered(player.getName()))
            player.sendMessage(ChatColor.RED + "Login with /login <password>");
        else if (osas.dc.hasLegacyData(player.getName()))
            player.sendMessage(ChatColor.RED + "Login with /login <password>");
        else
            player.sendMessage(ChatColor.RED + "Register with /register <password> <confirm>");
    }
}
