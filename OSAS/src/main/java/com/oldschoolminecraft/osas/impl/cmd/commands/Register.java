package com.oldschoolminecraft.osas.impl.cmd.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.oldschoolminecraft.osas.Util;
import com.oldschoolminecraft.osas.impl.cmd.Command;

public class Register extends Command
{
    public Register()
    {
        super("Register", "Register an account");
    }
    
    public boolean run(CommandSender sender, String[] args)
    {
        Player ply = (Player)sender;
        if (args.length < 2)
            return false;
        
        if (fm.isRegistered(ply.getName().toLowerCase()) || osas.dc.hasLegacyData(ply.getName().toLowerCase()))
        {
            fm.sendError(sender, "You are already registered!");
            return true;
        }
        
        String password = args[0].trim();
        String confirm = args[1].trim();
        
        if (!confirm.equals(password))
        {
            fm.sendError(sender, "Passwords did not match!");
            return true;
        }
        
        if (fm.isAuthenticated(ply.getName().toLowerCase()) && fm.isFrozen(ply.getName().toLowerCase()))
        {
            String[] hash = Util.hash(password);
            fm.registerPlayer(ply.getName().toLowerCase(), hash[0], hash[1], true);
            fm.unfreezePlayer(ply.getName().toLowerCase());
        } else if (!fm.isAuthenticated(ply.getName().toLowerCase())) {
            String[] hash = Util.hash(password);
            fm.registerPlayer(ply.getName().toLowerCase(), hash[0], hash[1], false);
            fm.authenticatePlayer(ply.getName().toLowerCase());
        }
        
        fm.sendSuccess(sender, "Successfully registered!");
        System.out.println(String.format("Player '%s' registered.", ply.getName().toLowerCase()));
        return true;
    }
}
