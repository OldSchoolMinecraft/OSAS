package net.oldschoolminecraft.osas;

import com.earth2me.essentials.Essentials;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class OSAS extends JavaPlugin
{
    private static final Gson gson = new Gson();

    public static OSAS instance;
    private PlayerTracker tracker;
    private Essentials essentialsHandle;

    public void onEnable()
    {
        instance = this;
        tracker = new PlayerTracker();

        try
        {
            essentialsHandle = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
        } catch (Exception ex) {
            System.out.println("OSAS did not detect Essentials installed. Using 0,0 in world 'world' as default spawn point for players.");
        }

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
            Player player = (Player) sender;

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
                    Location lastLogoutPos = account.getLastLogoutLocation();
                    if (lastLogoutPos != null)
                        player.teleport(lastLogoutPos);
                    restoreInventory(player, account.getInventory());
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

            Player player = (Player) sender;

            try
            {
                OfflineAccount account = new OfflineAccount(sender.getName());
                if (account.isRegistered())
                {
                    sender.sendMessage(ChatColor.RED + "You are already registered.");
                    return true;
                }
            } catch (AuthenticationException ex) {
                ex.printStackTrace(System.err);
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

                File dataFile = new File(getDataFolder(), "users/" + sender.getName().toLowerCase() + ".json");
                try (JsonWriter writer = new JsonWriter(new FileWriter(dataFile)))
                {
                    gson.toJson(model, AccountModel.class, writer);
                    sender.sendMessage(ChatColor.GREEN + "Successfully registered account.");
                }

                return true;
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
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

                    File dataFile = new File(getDataFolder(), "users/" + sender.getName().toLowerCase() + ".json");
                    try (FileWriter writer = new FileWriter(dataFile))
                    {
                        gson.toJson(model, AccountModel.class, writer);
                        sender.sendMessage(ChatColor.GREEN + "Successfully changed password.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Incorrect password.");
                    return true;
                }
            } catch (AuthenticationException ex) {
                sender.sendMessage(ChatColor.RED + ex.getMessage());
            } catch (IOException e) {
                e.printStackTrace(System.err);
                sender.sendMessage(ChatColor.RED + "An unknown error occurred while changing your password. Please report this to an administrator.");
            }
        }

        return false;
    }

    private void restoreInventory(Player player, PlayerInventory inventory)
    {
        player.getInventory().setContents(inventory.getContents());
        player.getInventory().setArmorContents(inventory.getArmorContents());
    }

    public boolean isEssentialsInstalled()
    {
        return essentialsHandle != null;
    }

    public Essentials getEssentials()
    {
        return essentialsHandle;
    }

    public void onDisable()
    {
        System.out.println("OSAS disabled");
    }
}
