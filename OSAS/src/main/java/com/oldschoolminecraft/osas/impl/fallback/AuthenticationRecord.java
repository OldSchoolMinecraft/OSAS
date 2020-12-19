package com.oldschoolminecraft.osas.impl.fallback;

public class AuthenticationRecord
{
    public final String username;
    public final String module;
    
    public AuthenticationRecord(String username, String module)
    {
        this.username = username;
        this.module = module;
    }
    
    @Override
    public String toString()
    {
        return username;
    }
}
