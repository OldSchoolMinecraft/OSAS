package com.oldschoolminecraft.osas.compat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;

import com.oldschoolminecraft.osas.OSAS;
import com.oldschoolminecraft.osas.Util;

import me.moderator_man.fo.user.UserMetadata;
import me.moderator_man.meridian.serial.FormatReader;

public class FakeOnlineDataConverter
{
    public boolean hasLegacyData(String username)
    {
        return Util.fileExists("fo-data/" + username + ".dat");
    }
    
    public boolean convert(String username, String password)
    {
        try
        {
            FormatReader<UserMetadata> reader = new FormatReader<UserMetadata>();
            UserMetadata meta = reader.read("fo-data/" + username + ".dat");
            
            if (meta.getPassword().equals(Util.sha256(password)))
            {
                String[] hash = Util.hash(password);
                OSAS.instance.fallbackManager.registerPlayer(username, hash[0], hash[1], false);
                System.out.println("Converted FakeOnline data for: " + username);
                return true;
            }
            return false;
        } catch (Exception ex) {
            System.out.println("Failed to convert FakeOnline data for: " + username);
            ex.printStackTrace();
            return false;
        }
    }

    public JSONObject convertToJSON(UserMetadata data)
    {
        JSONObject obj = new JSONObject();
        obj.put("password", data.getPassword());
        return obj;
    }

    public String readFile(String path, Charset encoding) throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
