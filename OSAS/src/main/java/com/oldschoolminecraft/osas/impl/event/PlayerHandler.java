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
                for (String hook_name : osas.manager.getHooksMap().keySet())
                {
                    Hook hook = osas.manager.getHooksMap().get(hook_name);
                    boolean result = hook.authenticate(username, ip);
                    System.out.println(String.format("OSAS/%s: Authentication result for '%s': %s", hook_name, username, result));
                    if (result)
                        authenticated = true;
                }
                
                if (authenticated)
                    event.allow();
                else {
                    //TODO: fallback authentication
                    event.disallow(Result.KICK_OTHER, "Authentication failed");
                }
                    
                
                event.removeConnectionPause(pause);
            } catch (IOException ex) {
                event.removeConnectionPause(pause);
                ex.printStackTrace();
            }
        });
    }
}
