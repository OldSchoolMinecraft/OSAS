package com.oldschoolminecraft.osas.impl.cmd.commands;

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
        
        if (!fm.isRegistered(ply.getName()) && osas.dc.hasLegacyData(ply.getName()))
        {
            if (osas.dc.convert(ply.getName(), args[0]))
            {
                if (fm.isAuthenticated(ply.getName()) && fm.isFrozen(ply.getName()))
                {
                    if (fm.isFrozen(ply.getName()))
                        fm.unfreezePlayer(ply.getName());
                    fm.approvePlayer(ply.getName());
                } else if (!fm.isAuthenticated(ply.getName())) {
                    fm.authenticatePlayer(ply.getName());
                }
                fm.sendSuccess(sender, "Successfully logged in!");
                System.out.println(String.format("Player '%s' logged in.", ply.getName()));
            } else {
                fm.sendError(sender, "Invalid password!");
            }
            return true;
        }
        
        if (!fm.isRegistered(ply.getName()))
        {
            fm.sendError(sender, "You must register first!");
            return true;
        }
        if (fm.isAuthenticated(ply.getName()) && !fm.isFrozen(ply.getName()))
        {
            fm.sendError(sender, "You are already logged in!");
            return true;
        }
        Account account = fm.getAccount(ply.getName());
        String inputPasswd = Util.hash(args[0], account.salt);
        if (inputPasswd.equals(account.password))
        {
            if (fm.isAuthenticated(ply.getName()) && fm.isFrozen(ply.getName()))
            {
                if (fm.isFrozen(ply.getName()))
                    fm.unfreezePlayer(ply.getName());
                fm.approvePlayer(ply.getName());
            } else if (!fm.isAuthenticated(ply.getName())) {
                fm.authenticatePlayer(ply.getName());
            }
            fm.sendSuccess(sender, "Successfully logged in!");
            System.out.println(String.format("Player '%s' logged in.", ply.getName()));
        } else {
            fm.sendError(sender, "Invalid password!");
        }
        return true;
    }
}
