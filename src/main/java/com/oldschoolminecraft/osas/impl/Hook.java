package com.oldschoolminecraft.osas.impl;

import java.io.IOException;

public interface Hook
{
    public boolean authenticate(String username, String ip) throws IOException;
}
