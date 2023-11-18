package net.oldschoolminecraft.osas;

import java.util.HashMap;

/**
 * This class tracks online & connected players in memory.
 */
public class PlayerTracker
{
    private final HashMap<String, OfflineAccount> offlineAccounts;

    public PlayerTracker()
    {
        offlineAccounts = new HashMap<>();
    }

    public void trackPlayer(OfflineAccount account)
    {
        offlineAccounts.put(account.getUsername().toLowerCase(), account);
    }

    public void untrackPlayer(String username)
    {
        offlineAccounts.remove(username.toLowerCase());
    }

    public OfflineAccount getTrackedAccount(String username)
    {
        return offlineAccounts.get(username.toLowerCase());
    }
}
