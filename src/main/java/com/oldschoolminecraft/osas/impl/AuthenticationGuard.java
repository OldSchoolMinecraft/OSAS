package com.oldschoolminecraft.osas.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Shared across sessions and usernames; only rejected passwords consume attempts. */
public class AuthenticationGuard
{
    public static final String BLOCK_MESSAGE = "Authentication blocked for this IP. Contact an administrator.";

    private final Server server;
    private final Logger logger;
    private final File stateFile;
    private final int maxFailures;
    private final int maxAccounts;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, FailureRecord> failures = new HashMap<String, FailureRecord>();
    private boolean storageFailed;

    public AuthenticationGuard(Server server, Logger logger, File stateFile, int maxFailures, int maxAccounts)
    {
        if (maxFailures < 1 || maxAccounts < 1)
            throw new IllegalArgumentException("Authentication thresholds must be positive");
        this.server = server;
        this.logger = logger;
        this.stateFile = stateFile;
        this.maxFailures = maxFailures;
        this.maxAccounts = maxAccounts;
        if (stateFile.exists())
        {
            try
            {
                Map<String, FailureRecord> stored = mapper.readValue(stateFile,
                        new TypeReference<Map<String, FailureRecord>>() {});
                if (stored == null)
                    throw new IOException("Missing authentication failure records");
                for (FailureRecord record : stored.values())
                    if (record == null || record.count < 1 || record.accounts == null
                            || record.accounts.isEmpty() || record.accounts.contains(null))
                        throw new IOException("Invalid authentication failure record");
                failures.putAll(stored);
            } catch (IOException ex) {
                failStorage(ex);
            }
        }
    }

    public static String addressOf(Player player)
    {
        InetSocketAddress address = player.getAddress();
        return address == null || address.getAddress() == null ? null : address.getAddress().getHostAddress();
    }

    public synchronized boolean isBlocked(String ip)
    {
        return ip == null || storageFailed || server.getIPBans().contains(ip);
    }

    /** Called on the server thread, where native bans and player kicks are safe. */
    public synchronized void recordFailure(Player player)
    {
        String ip = addressOf(player);
        if (isBlocked(ip))
        {
            player.kickPlayer(BLOCK_MESSAGE);
            return;
        }

        FailureRecord record = failures.get(ip);
        if (record == null)
        {
            record = new FailureRecord();
            failures.put(ip, record);
        }
        record.count++;
        record.accounts.add(player.getName().toLowerCase(Locale.ROOT));
        if (record.count >= maxFailures || record.accounts.size() >= maxAccounts)
        {
            server.banIP(ip);
            logger.warning("OSAS banned IP " + ip + " after " + record.count
                    + " failed passwords across " + record.accounts.size() + " accounts.");
            // Native bans own blocked state, so /pardon-ip permits a fresh start.
            failures.remove(ip);
            save();
            for (Player online : server.getOnlinePlayers())
                if (online != player && ip.equals(addressOf(online)))
                    online.kickPlayer(BLOCK_MESSAGE);
            player.kickPlayer(BLOCK_MESSAGE);
        } else {
            save();
            player.kickPlayer(storageFailed ? BLOCK_MESSAGE : "Invalid password!");
        }
    }

    private void save()
    {
        File pending = new File(stateFile.getPath() + ".tmp");
        try
        {
            Files.createDirectories(stateFile.toPath().toAbsolutePath().getParent());
            mapper.writeValue(pending, failures);
            try
            {
                Files.move(pending.toPath(), stateFile.toPath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(pending.toPath(), stateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            failStorage(ex);
        }
    }

    private void failStorage(IOException ex)
    {
        storageFailed = true;
        logger.log(Level.SEVERE, "OSAS cannot persist authentication failures; authentication is blocked "
                + "until storage is repaired and OSAS is restarted.", ex);
    }

    public static class FailureRecord
    {
        public int count;
        public Set<String> accounts = new HashSet<String>();
    }
}
