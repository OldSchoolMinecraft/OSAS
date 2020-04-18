package com.oldschoolminecraft.osas.util;

import org.json.JSONObject;

public class FakeOnlineDataConverter
{
    public void convertAll()
    {
        //TODO: loop through FakeOnline data files, then convert them to JSON and write them back to disk
    }
    
    public JSONObject convertToJSON(UserMetadata data)
    {
        JSONObject obj = new JSONObject();
        obj.put("password", data.getPassword());
        return obj;
    }
}
