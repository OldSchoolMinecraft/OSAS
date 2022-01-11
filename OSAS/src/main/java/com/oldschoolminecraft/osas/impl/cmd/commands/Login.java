package com.oldschoolminecraft.osas.impl.cmd.commands;

import com.oldschoolminecraft.osas.impl.event.PlayerAuthenticationEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.oldschoolminecraft.osas.Util;
import com.oldschoolminecraft.osas.impl.cmd.Command;
import com.oldschoolminecraft.osas.impl.fallback.Account;

public class Login extends Command
{
    public Login()
    {
        super("Login", "Login to your account");
    }

    public boolean run(CommandSender sender, String[] args)
    {
        Player ply = (Player) sender;
        
        if (!fm.isRegistered(ply.getName().toLowerCase()) && osas.dc.hasLegacyData(ply.getName().toLowerCase()))
        {
            if (osas.dc.convert(ply.getName().toLowerCase(), args[0]))
            {
                if (fm.isAuthenticated(ply.getName().toLowerCase()) && fm.isFrozen(ply.getName().toLowerCase()))
                {
                    if (fm.isFrozen(ply.getName().toLowerCase()))
                        fm.unfreezePlayer(ply.getName().toLowerCase());
                    fm.approvePlayer(ply.getName().toLowerCase());
                } else if (!fm.isAuthenticated(ply.getName().toLowerCase())) {
                    fm.authenticatePlayer(ply.getName().toLowerCase());
                }
                fm.sendSuccess(sender, "Successfully logged in!");
                System.out.println(String.format("Player '%s' logged in.", ply.getName().toLowerCase()));
                PlayerAuthenticationEvent authenticationEvent = new PlayerAuthenticationEvent(ply.getUniqueId(), true);
                Bukkit.getPluginManager().callEvent(authenticationEvent);
            } else {
                fm.sendError(sender, "Invalid password!");
            }
            return true;
        }
        
        if (!fm.isRegistered(ply.getName().toLowerCase()))
        {
            fm.sendError(sender, "You must register first!");
            return true;
        }
        
        if (fm.isAuthenticated(ply.getName().toLowerCase()) && !fm.isFrozen(ply.getName().toLowerCase()))
        {
            fm.sendError(sender, "You are already logged in!");
            return true;
        }
        
        Account account = fm.getAccount(ply.getName().toLowerCase());
        String inputPasswd = Util.hash(args[0], account.salt);
        if (inputPasswd.equals(account.password))
        {
            if (fm.isAuthenticated(ply.getName().toLowerCase()) && fm.isFrozen(ply.getName().toLowerCase()))
            {
                if (fm.isFrozen(ply.getName().toLowerCase()))
                    fm.unfreezePlayer(ply.getName().toLowerCase());
                fm.approvePlayer(ply.getName().toLowerCase());
            } else if (!fm.isAuthenticated(ply.getName().toLowerCase())) {
                fm.authenticatePlayer(ply.getName().toLowerCase());
            }
            fm.sendSuccess(sender, "Successfully logged in!");
            System.out.println(String.format("Player '%s' logged in.", ply.getName().toLowerCase()));
            PlayerAuthenticationEvent authenticationEvent = new PlayerAuthenticationEvent(ply.getUniqueId(), true);
            Bukkit.getPluginManager().callEvent(authenticationEvent);
        } else {
            fm.sendError(sender, "Invalid password!");
            ply.kickPlayer(ChatColor.RED + "Invalid password!");
        }
        return true;
    }
}
