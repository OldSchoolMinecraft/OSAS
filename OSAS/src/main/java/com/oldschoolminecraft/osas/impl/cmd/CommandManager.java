package com.oldschoolminecraft.osas.impl.cmd;

import com.oldschoolminecraft.osas.impl.AliasMap;
import com.oldschoolminecraft.osas.impl.cmd.commands.ChangePassword;
import com.oldschoolminecraft.osas.impl.cmd.commands.DeleteAccount;
import com.oldschoolminecraft.osas.impl.cmd.commands.Login;
import com.oldschoolminecraft.osas.impl.cmd.commands.Register;

public class CommandManager
{
    private AliasMap<String, Command> commands;
    
    public CommandManager()
    {
        commands = new AliasMap<String, Command>();
    }
    
    public void onEnable()
    {
        register("register", new Register());
        register("login", new Login());
        register("changepassword", new ChangePassword(), "cp", "passwd");
        register("deleteaccount", new DeleteAccount(), "delacc");
    }
    
    public void register(String realKey, Command command, String... aliases)
    {
        commands.put(realKey, command);
        for (String alias : aliases)
            commands.alias(realKey, alias);
    }
    
    public Command getCommand(String call)
    {
        return commands.get(call);
    }
}
