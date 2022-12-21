package com.oldschoolminecraft.osas.impl.cmd.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.oldschoolminecraft.osas.Util;
import com.oldschoolminecraft.osas.impl.cmd.Command;
import com.oldschoolminecraft.osas.impl.fallback.Account;

public class ChangePassword extends Command
{
    public ChangePassword()
    {
        super("ChangePassword", "Change your password");
    }

    @Override
    public boolean run(CommandSender sender, String[] args)
    {
        Player ply = (Player) sender;
        if (!fm.isRegistered(ply.getName().toLowerCase()))
        {
            fm.sendError(sender, "You must register first!");
            return true;
        }
        if (!fm.isAuthenticated(ply.getName().toLowerCase()))
        {
            fm.sendError(sender, "You must login first!");
            return true;
        }
        if (args.length < 2)
            return false;
        Account account = fm.getAccount(ply.getName().toLowerCase());
        String oldPassword = account.password;
        String newPassword = args[1];
        if (Util.hash(args[0], account.salt).equals(oldPassword))
        {
            String[] newp = Util.hash(newPassword);
            account.password = newp[0];
            account.salt = newp[1];
            account.approved = false;
            fm.updateAccount(account);
            fm.sendSuccess(sender, "Successfully changed password! You will need to login again.");
            fm.deauthenticatePlayer(ply.getName().toLowerCase());
            return true;
        }
        fm.sendError(sender, "Old password was incorrect!");
        return true;
    }
}
