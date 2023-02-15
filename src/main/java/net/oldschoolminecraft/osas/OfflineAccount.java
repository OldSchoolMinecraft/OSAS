package net.oldschoolminecraft.osas;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Objects;

public class OfflineAccount
{
    private String username;
    private String passwordHash;
    private String salt;
    private boolean loggedIn = false;

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
        ObjectMapper mapper = new ObjectMapper();
        AccountModel account = mapper.readValue(getAccountFile(), AccountModel.class);
        this.passwordHash = account.password;
        this.salt = account.salt;
//        System.out.println(String.format("Loaded account data for %s (pwd=%s, salt=%s)", username, passwordHash, salt));
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
}
