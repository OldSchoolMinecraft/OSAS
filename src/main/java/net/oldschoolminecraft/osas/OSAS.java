package net.oldschoolminecraft.osas;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class OSAS extends JavaPlugin
{
    public static OSAS instance;
    private PlayerTracker tracker;

    public void onEnable()
    {
        instance = this;
        tracker = new PlayerTracker();

        PlayerHandler playerHandler = new PlayerHandler(this, tracker);
        getServer().getPluginManager().registerSuperEvents(playerHandler, this);
        //getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerHandler, Event.Priority.Normal, this);

        System.out.println("OSAS enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (command.getLabel().equalsIgnoreCase("login"))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            if (args.length < 1)
            {
                sender.sendMessage(ChatColor.RED + "Usage: /login <password>");
                return true;
            }

            String password = args[0];
            if (password.isEmpty())
            {
                sender.sendMessage(ChatColor.RED + "Password cannot be empty.");
                return true;
            }
            OfflineAccount account = tracker.getTrackedAccount(sender.getName());
            if (account == null)
            {
                sender.sendMessage(ChatColor.RED + "There was an error while logging you in. Please report this to an administrator.");
                return true;
            }
            if (!account.isRegistered())
            {
                sender.sendMessage(ChatColor.RED + "You are not registered.");
                return true;
            }
            if (account.isLoggedIn())
            {
                sender.sendMessage(ChatColor.RED + "You are already logged in.");
                return true;
            }
            try
            {
                if (account.login(password))
                {
                    sender.sendMessage(ChatColor.GREEN + "Successfully logged in.");
                } else {
                    ((Player) sender).kickPlayer(ChatColor.RED + "Incorrect password.");
                }
                return true;
            } catch (AuthenticationException e) {
                sender.sendMessage(ChatColor.RED + e.getMessage());
                return true;
            }
        }

        if (command.getLabel().equalsIgnoreCase("register"))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            if (args.length < 1)
            {
                sender.sendMessage(ChatColor.RED + "Usage: /register <password>");
                return true;
            }

            try
            {
                OfflineAccount account = new OfflineAccount(sender.getName());
                if (account.isRegistered())
                {
                    sender.sendMessage(ChatColor.RED + "You are already registered.");
                    return true;
                }
            } catch (AuthenticationException ex) {
                ex.printStackTrace();
                sender.sendMessage(ChatColor.RED + "Failed to register account. Please report this to an administrator.");
                return true;
            }

            try
            {

                String password = args[0];
                if (password.isEmpty())
                {
                    sender.sendMessage(ChatColor.RED + "Password cannot be empty.");
                    return true;
                }
                String[] hash = Util.hash(password);
                assert hash != null;
                AccountModel model = new AccountModel();
                model.username = sender.getName().toLowerCase();
                model.password = hash[0];
                model.salt = hash[1];
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(new File(getDataFolder(), "users/" + sender.getName().toLowerCase() + ".json"), model);
                sender.sendMessage(ChatColor.GREEN + "Successfully registered account.");
                return true;
            } catch (IOException ex) {
                ex.printStackTrace();
                sender.sendMessage(ChatColor.RED + "Failed to register account. Please report this to an administrator.");
            }
        }

        if (command.getLabel().equalsIgnoreCase("deluser"))
        {
            if (sender.hasPermission("osas.deluser") || sender.isOp())
            {
                if (args.length < 1)
                {
                    sender.sendMessage(ChatColor.RED + "Usage: /deluser <username>");
                    return true;
                }
                String username = args[0];
                File file = new File(getDataFolder(), "users/" + username.toLowerCase() + ".json");
                if (file.exists())
                {
                    if (file.delete())
                    {
                        if (tracker.getTrackedAccount(username) != null)
                            tracker.getTrackedAccount(username).logout();
                        sender.sendMessage(ChatColor.GREEN + "Successfully deleted user.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Failed to delete user.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "User does not exist.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
        }

        if (command.getLabel().equalsIgnoreCase("passwd"))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }

            if (args.length < 2)
            {
                sender.sendMessage(ChatColor.RED + "Usage: /passwd <old> <new>");
                return true;
            }
            String old = args[0];
            String password = args[1];
            OfflineAccount account = tracker.getTrackedAccount(sender.getName());

            if (account == null)
            {
                sender.sendMessage(ChatColor.RED + "There was an error while changing your password. Please report this to an administrator.");
                return true;
            }

            try
            {
                if (account.login(old))
                {
                    String[] hash = Util.hash(password);
                    assert hash != null;
                    AccountModel model = new AccountModel();
                    model.username = sender.getName();
                    model.password = hash[0];
                    model.salt = hash[1];
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(new File(getDataFolder(), "users/" + sender.getName().toLowerCase() + ".json"), model);
                    sender.sendMessage(ChatColor.GREEN + "Successfully changed password.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Incorrect password.");
                    return true;
                }
            } catch (AuthenticationException ex) {
                sender.sendMessage(ChatColor.RED + ex.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                sender.sendMessage(ChatColor.RED + "An unknown error occurred while changing your password. Please report this to an administrator.");
            }
        }

        return false;
    }

    public void onDisable()
    {
        System.out.println("OSAS disabled");
    }
}
