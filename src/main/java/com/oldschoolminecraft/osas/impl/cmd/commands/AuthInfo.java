package com.oldschoolminecraft.osas.impl.cmd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.oldschoolminecraft.osas.impl.cmd.Command;
import com.oldschoolminecraft.osas.impl.fallback.AuthenticationRecord;

public class AuthInfo extends Command
{
    public AuthInfo()
    {
        super("AuthInfo", "Get auth info about a player", "osas.admin.info", false);
    }

    public boolean run(CommandSender sender, String[] args)
    {
        if (args.length-1 < 1)
        {
            sender.sendMessage(ChatColor.RED + "Invalid arguments");
            return true;
        }
        
        String username = args[1];
        
        AuthenticationRecord record = fm.getAuthenticationRecord(username);
        
        if (record != null)
        {
            sender.sendMessage(ChatColor.DARK_GRAY + "Username: " + ChatColor.GRAY + record.username);
            //sender.sendMessage(ChatColor.DARK_GRAY + "IP: " + ChatColor.GRAY + record.ip);
            sender.sendMessage(ChatColor.DARK_GRAY + "Module: " + ChatColor.GRAY + record.module);
            
        } else {
            sender.sendMessage(ChatColor.DARK_GRAY + "No records found");
            return true;
        }
        
        return true;
    }
}
