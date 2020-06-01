package com.oldschoolminecraft.osas.compat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldschoolminecraft.osas.Util;

import me.moderator_man.meridian.serial.FormatReader;

public class FakeOnlineDataConverter
{
    public void convert(String username)
    {
        try
        {
            FormatReader<UserMetadata> reader = new FormatReader<UserMetadata>();
            UserMetadata meta = reader.read("fo-data/" + username);
            
            JSONObject obj = convertToJSON(meta);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File(Util.getUsersDirectory() + "/" + username + ".json"), obj);
            System.out.println("Converted FakeOnline data for: " + username);
        } catch (Exception ex) {
            System.out.println("Failed to convert FakeOnline data for: " + username);
            ex.printStackTrace();
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
