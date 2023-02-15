package net.oldschoolminecraft.osas;

import com.projectposeidon.johnymuffin.ConnectionPause;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.*;

@SuppressWarnings("all")
public class PlayerHandler extends PlayerListener
{
    private OSAS osas;
    private PlayerTracker tracker;

    public PlayerHandler(OSAS osas, PlayerTracker tracker)
    {
        this.osas = osas;
        this.tracker = tracker;
    }

    @EventHandler
    public void onPlayerPreLogin(PlayerPreLoginEvent event)
    {
        String username = event.getName().toLowerCase();
        String ip = event.getAddress().getHostAddress();

        ConnectionPause pause = event.addConnectionPause(osas, "OSAS");
        Bukkit.getScheduler().scheduleAsyncDelayedTask(osas, () ->
        {
            try
            {
                OfflineAccount account = new OfflineAccount(username);
                tracker.trackPlayer(account);

                event.removeConnectionPause(pause);
            } catch (AuthenticationException ex) {
                ex.printStackTrace();
                event.disallow(PlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "Backend fault. Try again later.");
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerLoginEvent event) {
        handleFallbackAuth(event.getPlayer());
    }

    private void handleFallbackAuth(Player player)
    {
        if (player == null) return;
        String name = player.getName().toLowerCase();
        OfflineAccount account = tracker.getTrackedAccount(name);
        if (account == null) return;

        if (!account.isRegistered())
            sendDelayedMessage(player, ChatColor.RED + "Register with /register <password>", 10); //player.sendMessage(ChatColor.RED + "Register with /register <password>");
        else if (!account.isLoggedIn())
            sendDelayedMessage(player, ChatColor.RED + "Login with /login <password>", 10); //player.sendMessage(ChatColor.RED + "Login with /login <password>");
    }

    private void sendDelayedMessage(Player player, String message, long delay)
    {
        Bukkit.getScheduler().scheduleAsyncDelayedTask(osas, () ->
        {
            player.sendMessage(message);
        }, delay);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        try
        {
            tracker.untrackPlayer(event.getPlayer().getName().toLowerCase());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event)
    {
        final Player player = event.getPlayer();
        final Location fromLoc = event.getFrom();
        final Location toLoc = event.getTo();
        if (fromLoc.getX() == toLoc.getX() && fromLoc.getZ() == toLoc.getZ() && fromLoc.getY() > toLoc.getY())
            return;
        OfflineAccount account = tracker.getTrackedAccount(player.getName().toLowerCase());
        boolean stop = false;
        if (account == null) stop = true;
        if (!account.isLoggedIn()) stop = true;
        if (stop)
        {
            event.setCancelled(true);
            player.teleport(fromLoc);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event)
    {
        handleEvent(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event)
    {
        handleEvent(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event)
    {
        handleEvent(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event)
    {
        handleEvent(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerBucketFill(PlayerBucketFillEvent event)
    {
        handleEvent(event, event.getPlayer());
    }

    @EventHandler(priority = Event.Priority.Normal)
    public void onPlayerChat(PlayerChatEvent event)
    {
        OfflineAccount account = tracker.getTrackedAccount(event.getPlayer().getName().toLowerCase());
        if (account == null || !account.isLoggedIn())
        {
            event.setMessage("");
            event.setCancelled(true);
            handleFallbackAuth(event.getPlayer());
        }
    }

    @EventHandler(priority = Event.Priority.Normal)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        OfflineAccount account = tracker.getTrackedAccount(event.getPlayer().getName().toLowerCase());
        if (account == null) event.setCancelled(true);
        if (!account.isLoggedIn() || !account.isRegistered())
        {
            final String label = event.getMessage().split(" ")[0];
            if (label.isEmpty())
            {
                event.setCancelled(true);
                return; // no messages for empty commands
            }
            if (label.equalsIgnoreCase("/login") || label.equalsIgnoreCase("/register"))
            {
                System.out.println("Player " + event.getPlayer().getName() + " used allowed command whilst logged out: " + label);
                return;
            }
            System.out.println("Player " + event.getPlayer().getName() + " issued disallowed command whilst logged out: " + label);
            event.setMessage("");
            event.setCancelled(true);
            handleFallbackAuth(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        handleEvent(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        handleEvent(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event)
    {
        handleEvent(event, event.getPlayer());
    }

    private void handleEvent(Cancellable event, Player player)
    {
        OfflineAccount account = tracker.getTrackedAccount(player.getName().toLowerCase());
        if (!account.isRegistered())
        {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Register with /register <password>");
        } else if (!account.isLoggedIn()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Login with /login <password>");
        }
    }
}
