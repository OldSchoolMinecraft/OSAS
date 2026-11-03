package com.oldschoolminecraft.osas;

import com.oldschoolminecraft.osas.compat.FakeOnlineDataConverter;
import com.oldschoolminecraft.osas.impl.AuthenticationGuard;
import com.oldschoolminecraft.osas.impl.HookManager;
import com.oldschoolminecraft.osas.impl.cmd.commands.Login;
import com.oldschoolminecraft.osas.impl.event.PlayerHandler;
import com.oldschoolminecraft.osas.impl.fallback.Account;
import com.oldschoolminecraft.osas.impl.fallback.FallbackManager;
import com.projectposeidon.johnymuffin.ConnectionPause;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class AuthenticationProtectionTest
{
    private static final String ATTACKER = "192.0.2.10";
    private static final String OTHER = "192.0.2.11";
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private final Set<String> bans = new HashSet<String>();
    private final List<Player> online = new ArrayList<Player>();
    private final List<Runnable> tasks = new ArrayList<Runnable>();
    private final List<Object> events = new ArrayList<Object>();
    private final Map<Player, List<String>> kicks = new java.util.IdentityHashMap<Player, List<String>>();
    private Server server;
    private TestPlugin plugin;
    private TestAccounts accounts;
    private TestConverter converter;
    private TestLogin login;
    private File state;

    @Before public void setUp() throws Exception
    {
        BukkitScheduler scheduler = proxy(BukkitScheduler.class, (p, m, a) -> {
            if (m.getName().equals("scheduleAsyncDelayedTask")) {
                tasks.add((Runnable) a[1]);
                return tasks.size();
            }
            return null;
        });
        PluginManager plugins = proxy(PluginManager.class, (p, m, a) -> {
            if (m.getName().equals("callEvent")) events.add(a[0]);
            return null;
        });
        server = proxy(Server.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getIPBans": return new HashSet<String>(bans);
                case "banIP": bans.add((String) a[0]); return null;
                case "getOnlinePlayers": return online.toArray(new Player[0]);
                case "getScheduler": return scheduler;
                case "getPluginManager": return plugins;
                default: throw new AssertionError("Unexpected server call: " + m);
            }
        });
        Field bukkitServer = Bukkit.class.getDeclaredField("server");
        bukkitServer.setAccessible(true);
        bukkitServer.set(null, server);
        plugin = new TestPlugin();
        plugin.init(server, temp.getRoot());
        OSAS.instance = plugin;
        accounts = new TestAccounts();
        converter = new TestConverter();
        plugin.fallbackManager = accounts;
        plugin.dc = converter;
        plugin.manager = new HookManager();
        state = new File(temp.getRoot(), "authentication-failures.json");
        plugin.authenticationGuard = guard(3, 3);
        login = new TestLogin(plugin);
    }

    @Test public void thirdFailureBansAcrossReconnectsAndRestarts() throws Exception
    {
        for (int i = 1; i <= 3; i++) {
            plugin.authenticationGuard = guard(3, 3);
            Player reconnect = player("Alice", ATTACKER, 1000 + i);
            plugin.authenticationGuard.recordFailure(reconnect);
            assertEquals(i == 3, bans.contains(ATTACKER));
        }
        assertTrue(guard(3, 3).isBlocked(ATTACKER));
        bans.remove(ATTACKER); // /pardon-ip starts a fresh counter.
        AuthenticationGuard afterPardon = guard(3, 3);
        assertFalse(afterPardon.isBlocked(ATTACKER));
        afterPardon.recordFailure(player("Alice", ATTACKER, 2000));
        assertFalse(bans.contains(ATTACKER));
    }

    @Test public void failuresAcrossUsernamesShareOneIpCounter() throws Exception
    {
        for (String name : new String[] {"Alice", "Bob", "Carol"})
            plugin.authenticationGuard.recordFailure(player(name, ATTACKER, 1000));
        assertTrue(bans.contains(ATTACKER));
        assertFalse(plugin.authenticationGuard.isBlocked(OTHER));
    }

    @Test public void distinctAccountLimitIsIndependentAndIgnoresCaseAndPort() throws Exception
    {
        AuthenticationGuard guard = guard(10, 3);
        guard.recordFailure(player("Alice", ATTACKER, 1000));
        guard.recordFailure(player("ALICE", ATTACKER, 1001));
        guard.recordFailure(player("Bob", ATTACKER, 1002));
        assertFalse(bans.contains(ATTACKER));
        guard = guard(10, 3); // The distinct-account set also survives restart.
        guard.recordFailure(player("Carol", ATTACKER, 1003));
        assertTrue(bans.contains(ATTACKER));
    }

    @Test public void banningKicksOtherConnectedPlayersAtSameIp() throws Exception
    {
        Player peer = player("Bob", ATTACKER, 1001);
        Player innocent = player("Carol", OTHER, 1002);
        online.add(peer);
        online.add(innocent);
        AuthenticationGuard guard = guard(1, 3);
        guard.recordFailure(player("Alice", ATTACKER, 1000));
        assertFalse(kicks.get(peer).isEmpty());
        assertTrue(kicks.get(innocent).isEmpty());
    }

    @Test public void equivalentIpv6RepresentationsShareOneCounter() throws Exception
    {
        plugin.authenticationGuard.recordFailure(player("Alice", "2001:db8::1", 1000));
        plugin.authenticationGuard.recordFailure(player("Bob", "2001:0db8:0:0:0:0:0:1", 1001));
        assertTrue(bans.isEmpty());
        plugin.authenticationGuard.recordFailure(player("Carol", "2001:db8::1", 1002));
        assertTrue(bans.contains(InetAddress.getByName("2001:db8::1").getHostAddress()));
    }

    @Test public void modernAndLegacyPasswordFailuresShareTheGuard() throws Exception
    {
        addAccount("alice");
        login.run(player("Alice", ATTACKER, 1000), new String[] {"wrong"});
        converter.legacy = true;
        login.run(player("Bob", ATTACKER, 1001), new String[] {"wrong"});
        assertFalse(bans.contains(ATTACKER));
        login.run(player("Carol", ATTACKER, 1002), new String[] {"wrong"});
        assertTrue(bans.contains(ATTACKER));
        assertEquals(2, converter.conversions);
        assertFalse(accounts.isAuthenticated("alice"));
        assertTrue(events.isEmpty());
    }

    @Test public void validPasswordAuthenticatesAndDoesNotEraseEarlierFailures() throws Exception
    {
        addAccount("alice");
        Player player = player("Alice", ATTACKER, 1000);
        login.run(player, new String[] {"wrong"});
        login.run(player, new String[] {"correct"});
        assertTrue(accounts.isAuthenticated("alice"));
        assertEquals(1, events.size());
        accounts.deauthenticatePlayer("alice");
        login.run(player, new String[] {"wrong"});
        assertFalse(bans.contains(ATTACKER));
        login.run(player, new String[] {"wrong"});
        assertTrue(bans.contains(ATTACKER));
    }

    @Test public void legacySuccessRemainsAvailableWithoutConsumingFailures() throws Exception
    {
        converter.legacy = true;
        converter.valid = true;
        login.run(player("Alice", OTHER, 1000), new String[] {"correct"});
        assertTrue(accounts.isAuthenticated("alice"));
        assertEquals(1, events.size());
        assertFalse(state.exists());
        assertTrue(bans.isEmpty());
    }

    @Test public void loginPreservesExistingDefaultLocaleAccountNames() throws Exception
    {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            addAccount("\u0131an");
            login.run(player("Ian", OTHER, 1000), new String[] {"correct"});
            assertTrue(accounts.isAuthenticated("\u0131an"));
            assertEquals(1, events.size());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test public void frozenAccountCanStillBeApprovedWithValidPassword() throws Exception
    {
        addAccount("alice");
        accounts.authenticatePlayer("alice");
        accounts.freezePlayer("alice");
        login.run(player("Alice", OTHER, 1000), new String[] {"correct"});
        assertFalse(accounts.isFrozen("alice"));
        assertTrue(accounts.isApproved("alice"));
        assertEquals(1, events.size());
        assertFalse(state.exists());
    }

    @Test public void malformedUnregisteredAndAlreadyLoggedInCommandsDoNotCount() throws Exception
    {
        Player player = player("Alice", ATTACKER, 1000);
        assertFalse(login.run(player, new String[0]));
        login.run(player, new String[] {"wrong"});
        addAccount("alice");
        accounts.authenticatePlayer("alice");
        login.run(player, new String[] {"wrong"});
        assertFalse(state.exists());
        assertTrue(kicks.get(player).isEmpty());
    }

    @Test public void blockedConnectedPlayerCannotSubmitEvenAValidPassword() throws Exception
    {
        addAccount("alice");
        bans.add(ATTACKER);
        Player player = player("Alice", ATTACKER, 1000);
        login.run(player, new String[] {"correct"});
        assertFalse(accounts.isAuthenticated("alice"));
        assertFalse(kicks.get(player).isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test public void blockedPreloginDoesNotStartAuthenticationHooks() throws Exception
    {
        bans.add(ATTACKER);
        TestPreLogin event = new TestPreLogin("Alice", ATTACKER);
        new PlayerHandler().onPlayerPreLogin(event);
        assertEquals(PlayerPreLoginEvent.Result.KICK_BANNED, event.getResult());
        assertTrue(tasks.isEmpty());
    }

    @Test public void banDuringHookCancelsPoseidonPauseBeforeAuthenticating() throws Exception
    {
        plugin.manager.addHook((name, ip) -> { bans.add(ip); return true; }, "test");
        TestPreLogin event = new TestPreLogin("Alice", ATTACKER);
        new PlayerHandler().onPlayerPreLogin(event);
        tasks.remove(0).run();
        assertTrue(event.cancelled);
        assertTrue(event.released);
        assertFalse(accounts.isAuthenticated("alice"));
    }

    @Test public void lateCancellationClearsAuthenticationStateBeforeNextConnection() throws Exception
    {
        addAccount("alice");
        accounts.users.get("alice").approved = true;
        accounts.onApprovalCheck = () -> bans.add(ATTACKER);
        plugin.manager.addHook((name, ip) -> true, "test");
        TestPreLogin cancelled = new TestPreLogin("Alice", ATTACKER);
        new PlayerHandler().onPlayerPreLogin(cancelled);
        tasks.remove(0).run();
        assertTrue(cancelled.cancelled);
        assertTrue(cancelled.released);
        assertFalse(accounts.isAuthenticated("alice"));
        assertNull(accounts.getAuthenticationRecord("alice"));
        assertFalse(accounts.isFrozen("alice"));
        accounts.onApprovalCheck = null;
        plugin.manager.getHooksMap().clear();
        plugin.manager.addHook((name, ip) -> false, "test");
        new PlayerHandler().onPlayerPreLogin(new TestPreLogin("Alice", OTHER));
        tasks.remove(0).run();
        assertFalse(accounts.isAuthenticated("alice"));
    }

    @Test public void corruptLegacyDataDoesNotConsumePasswordFailures() throws Exception
    {
        String name = "osas" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Path directory = Paths.get("fo-data");
        boolean createdDirectory = !Files.exists(directory);
        Files.createDirectories(directory);
        Path legacy = directory.resolve(name + ".dat");
        Files.write(legacy, "corrupt legacy record".getBytes(StandardCharsets.UTF_8));
        plugin.dc = new FakeOnlineDataConverter();
        try {
            for (int i = 0; i < 3; i++)
                login.run(player(name, ATTACKER, 1000 + i), new String[] {"correct"});
            assertTrue(bans.isEmpty());
            assertFalse(state.exists());
            assertFalse(accounts.isAuthenticated(name));
        } finally {
            Files.deleteIfExists(legacy);
            if (createdDirectory) Files.delete(directory);
        }
    }

    @Test public void bannedJoinCannotEmitAuthenticationEvent() throws Exception
    {
        accounts.authenticatePlayer("alice");
        bans.add(ATTACKER);
        Player player = player("Alice", ATTACKER, 1000);
        new PlayerHandler().onPlayerJoin(new PlayerJoinEvent(player, "joined"));
        assertFalse(kicks.get(player).isEmpty());
        assertTrue(events.isEmpty());
    }

    @Test public void legitimateHookSuccessStillAuthenticates() throws Exception
    {
        plugin.manager.addHook((name, ip) -> true, "test");
        TestPreLogin event = new TestPreLogin("Alice", OTHER);
        new PlayerHandler().onPlayerPreLogin(event);
        tasks.remove(0).run();
        assertTrue(accounts.isAuthenticated("alice"));
        assertFalse(event.cancelled);
        assertTrue(event.released);
        assertFalse(state.exists());
    }

    @Test public void hookRejectionsDoNotCountAsFailedPasswords() throws Exception
    {
        plugin.manager.addHook((name, ip) -> false, "test");
        for (int i = 0; i < 3; i++) {
            TestPreLogin event = new TestPreLogin("Alice", ATTACKER);
            new PlayerHandler().onPlayerPreLogin(event);
            tasks.remove(0).run();
            tasks.clear(); // Discard the normal delayed fallback prompt.
            assertFalse(event.cancelled);
            assertTrue(event.released);
        }
        assertTrue(bans.isEmpty());
        assertFalse(state.exists());
    }

    @Test public void corruptOrUnwritableStateFailsClosed() throws Exception
    {
        Files.write(state.toPath(), "invalid JSON".getBytes(StandardCharsets.UTF_8));
        assertTrue(guard(3, 3).isBlocked(ATTACKER));
        File parentFile = temp.newFile("not-a-directory");
        AuthenticationGuard guard = new AuthenticationGuard(server, quietLogger(),
                new File(parentFile, "state.json"), 3, 3);
        guard.recordFailure(player("Alice", ATTACKER, 1000));
        assertTrue(guard.isBlocked(ATTACKER));
    }

    private AuthenticationGuard guard(int failures, int accounts)
    {
        return new AuthenticationGuard(server, quietLogger(), state, failures, accounts);
    }

    private Logger quietLogger()
    {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        return logger;
    }

    private void addAccount(String username)
    {
        String[] hash = Util.hash("correct");
        Account account = new Account();
        account.username = username;
        account.password = hash[0];
        account.salt = hash[1];
        accounts.users.put(username, account);
    }

    private Player player(String name, String ip, int port) throws Exception
    {
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(ip), port);
        List<String> messages = new ArrayList<String>();
        PlayerInventory inventory = proxy(PlayerInventory.class, (p, m, a) -> {
            if (m.getName().equals("getContents")) return new ItemStack[36];
            if (m.getName().equals("getArmorContents")) return new ItemStack[4];
            return null;
        });
        Player player = proxy(Player.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getName": return name;
                case "getAddress": return address;
                case "getUniqueId": return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
                case "getInventory": return inventory;
                case "kickPlayer": messages.add((String) a[0]); return null;
                case "sendMessage": return null;
                default: throw new AssertionError("Unexpected player call: " + m);
            }
        });
        kicks.put(player, messages);
        return player;
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler)
    {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    private static class TestPlugin extends OSAS
    {
        void init(Server server, File directory)
        {
            initialize(null, server, new PluginDescriptionFile("OSAS", "test", OSAS.class.getName()),
                    directory, new File(directory, "OSAS.jar"), getClass().getClassLoader());
        }
    }

    private static class TestLogin extends Login
    {
        TestLogin(OSAS plugin) { osas = plugin; fm = plugin.fallbackManager; }
    }

    private static class TestAccounts extends FallbackManager
    {
        final Map<String, Account> users = new HashMap<String, Account>();
        Runnable onApprovalCheck;
        @Override public boolean isRegistered(String name) { return users.containsKey(name.toLowerCase()); }
        @Override public Account getAccount(String name) { return users.get(name.toLowerCase()); }
        @Override public void approvePlayer(String name) { getAccount(name).approved = true; }
        @Override public boolean isApproved(String name) {
            if (onApprovalCheck != null) onApprovalCheck.run();
            return super.isApproved(name);
        }
    }

    private static class TestConverter extends FakeOnlineDataConverter
    {
        boolean legacy;
        boolean valid;
        int conversions;
        @Override public boolean hasLegacyData(String name) { return legacy; }
        @Override public boolean convert(String name, String password) { conversions++; return valid; }
    }

    private static class TestPreLogin extends PlayerPreLoginEvent
    {
        boolean cancelled;
        boolean released;
        TestPreLogin(String name, String ip) throws Exception { super(name, InetAddress.getByName(ip), null); }
        @Override public ConnectionPause addConnectionPause(Plugin plugin, String name) { return null; }
        @Override public void removeConnectionPause(ConnectionPause pause) { released = true; }
        @Override public void cancelPlayerLogin(String reason) { cancelled = true; }
    }
}
