package com.oldschoolminecraft.osas.compat;

import java.io.Serializable;

public class UserMetadata implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private String password;
    private boolean approved;
    
    public UserMetadata(String password, boolean approved)
    {
        this.password = password;
        this.approved = approved;
    }
    
    public String getPassword()
    {
        return password;
    }
    
    public void setPassword(String password)
    {
        this.password = password;
    }
    
    public boolean isApproved()
    {
        return approved;
    }
    
    public void setApproved(boolean flag)
    {
        this.approved = flag;
    }
}