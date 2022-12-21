package com.oldschoolminecraft.osas.impl;

import java.util.Collection;
import java.util.HashMap;

public class HookManager
{
    private HashMap<String, Hook> hooks;
    
    public HookManager()
    {
        hooks = new HashMap<String, Hook>();
    }
    
    public void addHook(Hook hook, String name)
    {
        hooks.put(name, hook);
    }
    
    public HashMap<String, Hook> getHooksMap()
    {
        return hooks;
    }
    
    public Collection<Hook> getHooks()
    {
        return hooks.values();
    }
}
