package com.oldschoolminecraft.osas;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import com.oldschoolminecraft.osas.compat.FakeOnlineDataConverter;
import com.oldschoolminecraft.osas.impl.HookManager;
import com.oldschoolminecraft.osas.impl.cmd.CommandManager;
import com.oldschoolminecraft.osas.impl.event.EntityHandler;
import com.oldschoolminecraft.osas.impl.event.PlayerHandler;
import com.oldschoolminecraft.osas.impl.fallback.FallbackManager;

public class OSAS extends JavaPlugin
{
    public static OSAS instance;

    public HookManager manager;
    public FakeOnlineDataConverter dc;
    public FallbackManager fallbackManager;
    public PlayerHandler playerHandler;
    public EntityHandler entityHandler;
    public CommandManager cmdm;
    
    public final boolean debugMode = true;
    
    public void onEnable()
    {
        instance = this;

        manager = new HookManager();
        dc = new FakeOnlineDataConverter();
        fallbackManager = new FallbackManager();
        playerHandler = new PlayerHandler();
        entityHandler = new EntityHandler();
        cmdm = new CommandManager();
        
        cmdm.onEnable();
        
        setup();
        
        // register player event handler
        getServer().getPluginManager().registerEvent(Type.PLAYER_PRELOGIN, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_QUIT, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_MOVE, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_DROP_ITEM, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_PICKUP_ITEM, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_BED_ENTER, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_BUCKET_EMPTY, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_BUCKET_FILL, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_CHAT, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_COMMAND_PREPROCESS, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_INTERACT, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_INTERACT_ENTITY, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_PORTAL, playerHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.PLAYER_JOIN, playerHandler, Priority.Normal, this);
        
        getServer().getPluginManager().registerEvent(Type.ENTITY_DAMAGE, entityHandler, Priority.Normal, this);
        getServer().getPluginManager().registerEvent(Type.ENTITY_TARGET, entityHandler, Priority.Normal, this);
        
        System.out.println("OSAS enabled.");
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        String command = cmd.getName().toLowerCase();
        com.oldschoolminecraft.osas.impl.cmd.Command cmnd = this.cmdm.getCommand(command);
        if (cmnd == null)
            return false;
        if (cmnd.isPlayerOnly() && !(sender instanceof Player)) {
            fallbackManager.sendError(sender, "Only players can use this command!");
            return true;
        }
        if (cmnd.requiresPermission())
        {
            if (sender.isOp())
            {
                cmnd.run(sender, args);
                return true;
            }
            
            if (sender.hasPermission(cmnd.getPermission()))
            {
                cmnd.run(sender, args);
                return true;
            }
            
            fallbackManager.sendError(sender, "You do not have permission to use this command!");
            return true;
        }
        return cmnd.run(sender, args);
    }
    
    private void setup()
    {
        if (!Util.directoryExists(Util.getPluginDirectory()))
            Util.createDirectory(Util.getPluginDirectory());
        if (!Util.directoryExists(Util.getUsersDirectory()))
            Util.createDirectory(Util.getUsersDirectory());
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
