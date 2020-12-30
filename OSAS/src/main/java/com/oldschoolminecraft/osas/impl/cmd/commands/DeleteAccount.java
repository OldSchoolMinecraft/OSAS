package com.oldschoolminecraft.osas.impl.cmd.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.oldschoolminecraft.osas.impl.cmd.Command;

public class DeleteAccount extends Command
{
    public DeleteAccount()
    {
        super("DeleteAccount", "Delete an account", "osas.admin.delete", false);
    }

    @Override
    public boolean run(CommandSender sender, String[] args)
    {
        if (args.length < 1)
        {
            return false;
        }
        String username = args[0];
        if (!fm.isRegistered(username.toLowerCase()))
        {
            fm.sendError(sender, "That player doesn't have an account!");
            return true;
        }
        fm.deleteAccount(username);
        for (Player on : osas.getServer().getOnlinePlayers())
        {
            if (on.getName().equalsIgnoreCase(username))
                fm.deauthenticatePlayer(username);
        }
        fm.sendSuccess(sender, String.format("Successfully deleted account of '%s'!", username));
        return true;
    }
}