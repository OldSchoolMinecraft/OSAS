package com.oldschoolminecraft.osas.impl.event;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;

import com.oldschoolminecraft.osas.OSAS;
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

    private void scheduleAfterJoin(String username)
    {
        Bukkit.getScheduler().scheduleAsyncDelayedTask((Plugin) OSAS.instance, (Runnable) new Runnable()
        {
            @Override
            public void run()
            {
                fm.handleFallbackForPlayer(username);
            }
        }, 1L);
    }
}
