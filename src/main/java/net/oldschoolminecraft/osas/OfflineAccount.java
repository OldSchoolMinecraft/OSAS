package net.oldschoolminecraft.osas;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Objects;

public class OfflineAccount
{
    private static final Gson gson = new Gson();

    private String username;
    private String passwordHash;
    private String salt;
    private boolean loggedIn = false;
    private Location lastLogoutLocation;
    private PlayerInventory inventory;

    public OfflineAccount(String username) throws AuthenticationException
    {
        this.username = username.toLowerCase();

        //if (!isRegistered()) throw new AuthenticationException("Account not registered");

        try
        {
            if (isRegistered()) loadData();
        } catch (Exception e) {
            throw new AuthenticationException("Failed to load account data");
        }
    }

    public boolean login(String passwordRaw) throws AuthenticationException
    {
        if (loggedIn) throw new AuthenticationException("Already logged in");
        if (passwordRaw == null || passwordRaw.isEmpty()) throw new AuthenticationException("Password cannot be empty.");

        String passwordHash = Util.hash(passwordRaw, salt);

        loggedIn = Objects.requireNonNull(passwordHash).equals(this.passwordHash);
        return loggedIn;
    }

    public void logout()
    {
        loggedIn = false;
    }

    public File getAccountFile()
    {
        return new File(OSAS.instance.getDataFolder(), "users/" + username + ".json");
    }

    public boolean isRegistered()
    {
        return getAccountFile().exists();
    }

    public boolean isLoggedIn()
    {
        return loggedIn;
    }

    private void loadData() throws Exception
    {
        try (JsonReader reader = new JsonReader(new FileReader(getAccountFile())))
        {
            AccountModel acc = gson.fromJson(reader, AccountModel.class);
            this.passwordHash = acc.password;
            this.salt = acc.salt;
            this.lastLogoutLocation = acc.lastLogoutLocation != null ? acc.lastLogoutLocation.getBukkitLocation() : null;
        }
    }

    public String getUsername()
    {
        return username;
    }

    public String getPasswordHash()
    {
        return passwordHash;
    }

    public String getSalt()
    {
        return salt;
    }

    public Location getLastLogoutLocation()
    {
        return lastLogoutLocation;
    }

    public void setLastLogoutLocation(Location location)
    {
        this.lastLogoutLocation = location;
    }

    public PlayerInventory getInventory()
    {
        return inventory;
    }

    public void setInventory(PlayerInventory inv)
    {
        this.inventory = inv;
    }
}
