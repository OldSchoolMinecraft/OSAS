package com.oldschoolminecraft.osas.impl.cmd.commands;

import com.oldschoolminecraft.osas.impl.event.PlayerAuthenticationEvent;
import com.oldschoolminecraft.osas.impl.AuthenticationGuard;
import com.oldschoolminecraft.osas.compat.FakeOnlineDataConverter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.oldschoolminecraft.osas.Util;
import com.oldschoolminecraft.osas.impl.cmd.Command;
import com.oldschoolminecraft.osas.impl.fallback.Account;

import java.util.Objects;

public class Login extends Command
{
    public Login()
    {
        super("Login", "Login to your account");
    }

    public boolean run(CommandSender sender, String[] args)
    {
        Player ply = (Player) sender;
        if (osas.authenticationGuard.isBlocked(AuthenticationGuard.addressOf(ply)))
        {
            ply.kickPlayer(AuthenticationGuard.BLOCK_MESSAGE);
            return true;
        }
        if (args.length < 1)
            return false;
        
        if (!fm.isRegistered(ply.getName().toLowerCase()) && osas.dc.hasLegacyData(ply.getName().toLowerCase()))
        {
            boolean converted;
            try
            {
                converted = osas.dc.convert(ply.getName().toLowerCase(), args[0]);
            } catch (FakeOnlineDataConverter.ConversionException ex) {
                Util.loadInventory(ply);
                fm.sendError(sender, "Unable to read your legacy account. Contact an administrator.");
                ply.kickPlayer("Authentication temporarily unavailable. Contact an administrator.");
                return true;
            }
            if (converted)
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
                System.out.println(String.format("Player '%s' logged in with valid password. (FakeOnline data converted)", ply.getName().toLowerCase()));
                Util.loadInventory(ply);
                PlayerAuthenticationEvent authenticationEvent = new PlayerAuthenticationEvent(ply.getUniqueId(), true);
                Bukkit.getPluginManager().callEvent(authenticationEvent);
            } else {
                Util.loadInventory(ply);
                fm.sendError(sender, "Invalid password!");
                osas.authenticationGuard.recordFailure(ply);
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
        if (Objects.equals(inputPasswd, account.password))
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
            System.out.println(String.format("Player '%s' logged in with valid password.", ply.getName().toLowerCase()));
            Util.loadInventory(ply);
            Util.saveInventory(ply, true);
            PlayerAuthenticationEvent authenticationEvent = new PlayerAuthenticationEvent(ply.getUniqueId(), true);
            Bukkit.getPluginManager().callEvent(authenticationEvent);
        } else {
            Util.loadInventory(ply);
            fm.sendError(sender, "Invalid password!");
            osas.authenticationGuard.recordFailure(ply);
        }
        return true;
    }
}
