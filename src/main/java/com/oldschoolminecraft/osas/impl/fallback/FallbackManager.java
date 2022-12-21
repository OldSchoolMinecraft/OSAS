package com.oldschoolminecraft.osas.impl.fallback;

import java.io.File;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldschoolminecraft.osas.OSAS;
import com.oldschoolminecraft.osas.Util;

public class FallbackManager
{
    private ArrayList<AuthenticationRecord> authenticationRecords;
    
    private ArrayList<String> authenticatedPlayers;
    private ArrayList<String> frozenPlayers;
    
    public FallbackManager()
    {
        this.authenticationRecords = new ArrayList<AuthenticationRecord>();
        
        this.authenticatedPlayers = new ArrayList<String>();
        this.frozenPlayers = new ArrayList<String>();
    }
    
    public void handleFallbackForPlayer(String username)
    {
        Player player = Bukkit.getPlayer(username.toLowerCase());
        String name = username.toLowerCase();
        
        if (isAuthenticated(name))
        {
            if (isRegistered(name))
            {
                if (isApproved(name))
                {
                    sendSuccess((CommandSender) player, "You are authenticated, no login required!");
                } else {
                    sendError((CommandSender) player, "Login with /login <password>");
                    player.sendMessage(ChatColor.AQUA + "You will only have to do this once!");
                }
            } else {
                if (OSAS.instance.dc.hasLegacyData(username))
                    sendError((CommandSender) player, "Login with /login <password>");
                else {
                    sendError((CommandSender) player, "Register with /register <password> <confirm>");
                    player.sendMessage(ChatColor.AQUA + "You will NOT be required to login when you join!");
                }
            }
        } else if (isRegistered(name)) {
            sendError((CommandSender) player, "Login with /login <password>");
        } else {
            if (OSAS.instance.dc.hasLegacyData(username))
                sendError((CommandSender) player, "Login with /login <password>");
            else {
                sendError((CommandSender) player, "Register with /register <password> <confirm>");
                sendError((CommandSender) player, "You WILL be required to login when you join!");
            }
        }
    }
    
    public void sendError(CommandSender sender, String msg)
    {
        if (sender != null)
            sender.sendMessage(ChatColor.RED + msg);
    }
    
    public void sendSuccess(CommandSender sender, String msg)
    {
        if (sender != null)
            sender.sendMessage(ChatColor.GREEN + msg);
    }
    
    public boolean isRegistered(String username)
    {
        username = username.toLowerCase();
        return Util.fileExists(Util.getUsersDirectory() + "/" + username + ".json");
    }
    
    public boolean isAuthenticated(String username)
    {
        for (AuthenticationRecord rec : authenticationRecords)
            if (rec.username.toLowerCase().equals(username.toLowerCase()))
                return true;
        
        username = username.toLowerCase();
        return authenticatedPlayers.contains(username);
    }
    
    public boolean isFrozen(String username)
    {
        username = username.toLowerCase();
        return frozenPlayers.contains(username);
    }
    
    public boolean isApproved(String username)
    {
        username = username.toLowerCase();
        return getAccount(username).approved;
    }
    
    public void addAuthenticationRecord(String username, String module)
    {
        authenticationRecords.add(new AuthenticationRecord(username, module));
    }
    
    public void removeAuthenticationRecord(String username)
    {
        for (AuthenticationRecord rec : authenticationRecords)
            if (rec.username.toLowerCase().equals(username.toLowerCase()))
                authenticationRecords.remove(rec);
    }
    
    public AuthenticationRecord getAuthenticationRecord(String username)
    {
        for (AuthenticationRecord rec : authenticationRecords)
            if (rec.username.toLowerCase().equals(username.toLowerCase()))
                return rec;
        return null;
    }
    
    public void authenticatePlayer(String username)
    {
        username = username.toLowerCase();
        authenticatedPlayers.add(username);
    }
    
    public void deauthenticatePlayer(String username)
    {
        username = username.toLowerCase();
        authenticatedPlayers.remove(username);
    }
    
    public void freezePlayer(String username)
    {
        username = username.toLowerCase();
        frozenPlayers.add(username);
    }
    
    public void unfreezePlayer(String username)
    {
        username = username.toLowerCase();
        frozenPlayers.remove(username);
    }
    
    public void approvePlayer(String username)
    {
        username = username.toLowerCase();
        Account account = getAccount(username);
        deleteAccount(username);
        registerPlayer(username, account.password, account.salt, true);
    }
    
    public void updateAccount(Account account)
    {
        deleteAccount(account.username);
        registerPlayer(account.username, account.password, account.salt, account.approved);
    }
    
    public void registerPlayer(String username, String password, String salt, boolean approved)
    {
        try
        {
            username = username.toLowerCase();
            
            Account account = new Account();
            account.username = username;
            account.password = password;
            account.salt = salt;
            account.approved = approved;
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File(Util.getUsersDirectory() + "/" + username + ".json"), account);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public Account getAccount(String username)
    {
        try
        {
            username = username.toLowerCase();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(Util.getUsersDirectory() + "/" + username + ".json"), Account.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public void deleteAccount(String username)
    {
        username = username.toLowerCase();
        if (isRegistered(username))
            new File(Util.getUsersDirectory() + "/" + username + ".json").delete();
    }
}
