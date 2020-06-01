package com.oldschoolminecraft.osas;

import java.io.File;

public class Util
{
    public static boolean directoryExists(String path)
    {
        File file = new File(path);
        return file.exists() && file.isDirectory();
    }
    
    public static boolean fileExists(String path)
    {
        File file = new File(path);
        return file.exists() && !file.isDirectory();
    }
    
    public static String getUsersDirectory()
    {
        return getPluginDirectory() + "/users";
    }
    
    public static String getPluginDirectory()
    {
        return "plugins/OSAS";
    }
    
    public static void createDirectory(String path)
    {
        new File(path).mkdir();
    }
}
