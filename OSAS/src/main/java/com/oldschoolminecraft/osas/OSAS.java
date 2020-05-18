package com.oldschoolminecraft.osas;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import com.oldschoolminecraft.osas.impl.HookManager;
import com.oldschoolminecraft.osas.impl.event.PlayerHandler;

public class OSAS extends JavaPlugin
{
    public static OSAS instance;

    public HookManager manager;
    public PlayerHandler playerHandler;
    public final boolean debugMode = true;
    
    public void onEnable()
    {
        instance = this;

        manager = new HookManager();
        playerHandler = new PlayerHandler();

        // register player event handler
        getServer().getPluginManager().registerEvent(Type.PLAYER_PRELOGIN, playerHandler, Priority.Normal, this);
        
        System.out.println("OSAS enabled.");
    }

    public String get(String url)
    {
        try
        {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
            in.close();
            return response.toString();
        } catch (Exception ex) {
            JSONObject obj = new JSONObject();
            obj.append("error", ex.getMessage());
            return obj.toString();
        }
    }

    public void onDisable()
    {
        System.out.println("OSAS disabled.");
    }
}
