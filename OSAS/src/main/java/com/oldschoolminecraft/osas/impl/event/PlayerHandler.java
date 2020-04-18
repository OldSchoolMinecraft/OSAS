package com.oldschoolminecraft.osas.impl.event;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent.Result;

import com.oldschoolminecraft.osas.OSAS;
import com.oldschoolminecraft.osas.impl.Hook;
import com.projectposeidon.johnymuffin.ConnectionPause;

public class PlayerHandler extends PlayerListener
{
    private OSAS osas = OSAS.instance;
    
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
                for (Hook hook : osas.manager.getHooks())
                    if (hook.authenticate(username, ip))
                        authenticated = true;
                if (authenticated)
                    event.allow();
                else
                    event.disallow(Result.KICK_OTHER, "Authentication failed");
            } catch (IOException ex) {
                event.removeConnectionPause(pause);
                ex.printStackTrace();
            }
        });
        event.removeConnectionPause(pause);
    }
}
