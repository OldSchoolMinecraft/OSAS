package com.oldschoolminecraft.osas.impl.cmd;

import org.bukkit.command.CommandSender;

import com.oldschoolminecraft.osas.OSAS;
import com.oldschoolminecraft.osas.impl.fallback.FallbackManager;

public abstract class Command
{
    protected static OSAS osas = OSAS.instance;
    protected static FallbackManager fm = osas.fallbackManager;
    private String name;
    private String desc;
    private boolean requiresPermission;
    private boolean playerOnly;
    private String permission;
    
    public Command(String name, String desc)
    {
        this.name = name;
        this.desc = desc;
        this.requiresPermission = false;
        this.playerOnly = true;
    }
    
    public Command(String name, String desc, String permission)
    {
        this.name = name;
        this.desc = desc;
        this.requiresPermission = true;
        this.permission = permission;
    }
    
    public Command(String name, String desc, String permission, boolean playerOnly)
    {
        this.name = name;
        this.desc = desc;
        this.requiresPermission = true;
        this.permission = permission;
        this.playerOnly = playerOnly;
    }
    
    public abstract boolean run(CommandSender sender, String[] args);
    
    public String getName()
    {
        return name;
    }
    
    public String getDesc()
    {
        return desc;
    }
    
    public boolean requiresPermission()
    {
        return requiresPermission;
    }
    
    public boolean isPlayerOnly()
    {
        return playerOnly;
    }
    
    public String getPermission()
    {
        return permission;
    }
}
