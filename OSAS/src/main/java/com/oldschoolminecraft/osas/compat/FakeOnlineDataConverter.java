package com.oldschoolminecraft.osas.compat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import me.moderator_man.meridian.serial.FormatReader;

public class FakeOnlineDataConverter
{
    public void convertAll()
    {
        try
        {
            for (File f : new File("fo-data").listFiles())
            {
                FormatReader<UserMetadata> reader = new FormatReader<UserMetadata>();
                UserMetadata meta = reader.read(f.getAbsolutePath());
                JSONObject con = convertToJSON(meta);
                ObjectMapper mapper = new ObjectMapper();
                
            }

            // TODO: loop through FakeOnline data files, then convert them to JSON and write
            // them back to disk
        } catch (Exception ex) {
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
