package com.oldschoolminecraft.osas.impl.event;

import java.io.IOException;

import com.projectposeidon.johnymuffin.ConnectionPause;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;

import com.oldschoolminecraft.osas.OSAS;
import com.oldschoolminecraft.osas.Util;
import com.oldschoolminecraft.osas.impl.Hook;
import com.oldschoolminecraft.osas.impl.AuthenticationGuard;
import com.oldschoolminecraft.osas.impl.fallback.FallbackManager;

@SuppressWarnings("all")
public class PlayerHandler extends PlayerListener
{
    private OSAS osas = OSAS.instance;
    private FallbackManager fm = osas.fallbackManager;

    public void onPlayerItemDamage(PlayerItemDamageEvent event)
    {
        event.setCancelled(true); // nope.avi
    }

    public void onPlayerPreLogin(PlayerPreLoginEvent event)
    {
        String username = event.getName().toLowerCase();
        String ip = event.getAddress().getHostAddress();

        if (osas.authenticationGuard.isBlocked(ip))
        {
            event.disallow(PlayerPreLoginEvent.Result.KICK_BANNED, AuthenticationGuard.BLOCK_MESSAGE);
            return;
        }
        if (event.getResult() != PlayerPreLoginEvent.Result.ALLOWED)
            return;

        ConnectionPause pause = event.addConnectionPause(osas, "OSAS");
        Bukkit.getScheduler().scheduleAsyncDelayedTask(osas, () ->
        {
            boolean hookStateCreated = false;
            try
            {
                boolean authenticated = false;
                String module = "Fallback";
                for (String hook_name : osas.manager.getHooksMap().keySet())
                {
                    Hook hook = osas.manager.getHooksMap().get(hook_name);
                    boolean result = hook.authenticate(username, ip);
                    System.out.println(String.format("OSAS/%s: Authentication result for '%s': %s", hook_name, username, result));
                    if (result)
                    {
                        authenticated = true;
                        module = hook_name;
                    }
                }

                if (osas.authenticationGuard.isBlocked(ip))
                    return;

                if (authenticated)
                {
                    event.allow();
                    
                    // authenticate player
                    hookStateCreated = true;
                    fm.authenticatePlayer(username);
                    
                    // log auth info
                    fm.addAuthenticationRecord(username, module);
                    
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

            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try
                {
                    // Poseidon does not re-read the result or fire quit for a cancelled prelogin.
                    if (osas.authenticationGuard.isBlocked(ip))
                    {
                        event.cancelPlayerLogin(AuthenticationGuard.BLOCK_MESSAGE);
                        if (hookStateCreated)
                        {
                            fm.deauthenticatePlayer(username);
                            fm.removeAuthenticationRecord(username);
                            fm.unfreezePlayer(username);
                        }
                    }
                } finally {
                    event.removeConnectionPause(pause);
                }
            }
        });
    }

    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (osas.authenticationGuard.isBlocked(AuthenticationGuard.addressOf(event.getPlayer())))
        {
            event.getPlayer().kickPlayer(AuthenticationGuard.BLOCK_MESSAGE);
            return;
        }
        //Send authentication event
        if(fm.isAuthenticated(event.getPlayer().getName().toLowerCase())) {
            PlayerAuthenticationEvent authenticationEvent = new PlayerAuthenticationEvent(event.getPlayer().getUniqueId(), true);
            Bukkit.getPluginManager().callEvent(authenticationEvent);
            return;
        }

        Util.saveInventory(event.getPlayer(), false);
    }

    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        try
        {
            if (!fm.isAuthenticated(event.getPlayer().getName().toLowerCase()))
                Util.loadInventory(event.getPlayer());

            fm.deauthenticatePlayer(event.getPlayer().getName().toLowerCase());
            fm.removeAuthenticationRecord(event.getPlayer().getName().toLowerCase());
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
        if (!fm.isAuthenticated(player.getName().toLowerCase()) || fm.isFrozen(player.getName().toLowerCase()))
        {
            this.warn(event.getPlayer());
            event.setCancelled(true);
            player.teleport(fromLoc);
        }
    }

    public void onPlayerDropItem(final PlayerDropItemEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName().toLowerCase()) || fm.isFrozen(event.getPlayer().getName().toLowerCase()))
            event.setCancelled(true);
    }

    public void onPlayerPickupItem(final PlayerPickupItemEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName().toLowerCase()) || fm.isFrozen(event.getPlayer().getName().toLowerCase()))
            event.setCancelled(true);
    }

    public void onPlayerBedEnter(final PlayerBedEnterEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName().toLowerCase()) || fm.isFrozen(event.getPlayer().getName().toLowerCase()))
            event.setCancelled(true);
    }

    public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName().toLowerCase()) || fm.isFrozen(event.getPlayer().getName().toLowerCase()))
            event.setCancelled(true);
    }

    public void onPlayerBucketFill(final PlayerBucketFillEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName().toLowerCase()) || fm.isFrozen(event.getPlayer().getName().toLowerCase()))
            event.setCancelled(true);
    }

    public void onPlayerChat(final PlayerChatEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName().toLowerCase()) || fm.isFrozen(event.getPlayer().getName().toLowerCase()))
        {
            event.setMessage("");
            event.setCancelled(true);
            this.warn(event.getPlayer());
        }
    }

    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName().toLowerCase()) || fm.isFrozen(event.getPlayer().getName().toLowerCase()))
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
        if (!fm.isAuthenticated(event.getPlayer().getName().toLowerCase()) || fm.isFrozen(event.getPlayer().getName().toLowerCase()))
            event.setCancelled(true);
    }

    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName().toLowerCase()) || fm.isFrozen(event.getPlayer().getName().toLowerCase()))
            event.setCancelled(true);
    }

    public void onPlayerPortal(final PlayerPortalEvent event)
    {
        if (!fm.isAuthenticated(event.getPlayer().getName().toLowerCase()) || fm.isFrozen(event.getPlayer().getName().toLowerCase()))
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
        if (fm.isRegistered(player.getName().toLowerCase()))
            player.sendMessage(ChatColor.RED + "Login with /login <password>");
        else if (osas.dc.hasLegacyData(player.getName().toLowerCase()))
            player.sendMessage(ChatColor.RED + "Login with /login <password>");
        else
            player.sendMessage(ChatColor.RED + "Register with /register <password> <confirm>");
    }
}
