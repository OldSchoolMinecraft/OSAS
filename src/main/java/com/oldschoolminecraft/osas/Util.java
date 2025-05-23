package com.oldschoolminecraft.osas;

import com.google.gson.*;
import com.oldschoolminecraft.osas.impl.fallback.FallbackManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Util
{
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void saveInventory(Player ply, boolean backup)
    {
        FallbackManager fbm = OSAS.instance.fallbackManager;
        ItemStack[] items = ply.getInventory().getContents();
        ItemStack[] armor = ply.getInventory().getArmorContents();
        Util.saveInventory(items, armor, ply.getName().toLowerCase() + (backup ? ".bak.json" : ".json"));
        if (!backup) ply.getInventory().clear();
    }

    public static void saveInventory(ItemStack[] items, ItemStack[] armor, String fileName)
    {
        JsonObject root = new JsonObject();

        JsonArray mainItemsArray = new JsonArray();
        JsonArray armorItemsArray = new JsonArray();

        for (ItemStack item : items)
        {
            if (item != null)
            {
                JsonObject jsonItem = new JsonObject();
                jsonItem.addProperty("material", item.getType().toString());
                jsonItem.addProperty("amount", item.getAmount());
                jsonItem.addProperty("damage", item.getDurability());

                mainItemsArray.add(jsonItem);
            }
        }

        for (ItemStack item : armor)
        {
            if (item != null)
            {
                JsonObject jsonItem = new JsonObject();
                jsonItem.addProperty("material", item.getType().toString());
                jsonItem.addProperty("amount", item.getAmount());
                jsonItem.addProperty("damage", item.getDurability());

                armorItemsArray.add(jsonItem);
            }
        }

        root.add("main", mainItemsArray);
        root.add("armor", armorItemsArray);

        File targetFile = new File(OSAS.instance.getDataFolder(), "inventories/" + fileName);
        if (!targetFile.getParentFile().exists()) targetFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(targetFile))
        {
            gson.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public static void loadInventory(Player player)
    {
        File targetFile = new File(OSAS.instance.getDataFolder(), "inventories/" + player.getName().toLowerCase() + ".json");
        if (!targetFile.exists())
        {
            System.err.println("[OSAS] INVENTORY NOT FOUND FOR: " + player.getName());
            return;
        }

        try (FileReader reader = new FileReader(targetFile))
        {
            JsonObject root = gson.fromJson(reader, JsonObject.class);

            // Load main inventory items
            JsonArray mainItemsArray = root.getAsJsonArray("main");
            ItemStack[] mainItems = new ItemStack[36]; // Assuming 36 slots for main inventory
            for (int i = 0; i < mainItemsArray.size(); i++)
            {
                JsonObject jsonItem = mainItemsArray.get(i).getAsJsonObject();
                String material = jsonItem.get("material").getAsString();
                int amount = jsonItem.get("amount").getAsInt();
                short damage = jsonItem.has("damage") ? jsonItem.get("damage").getAsShort() : 0;

                Material itemMaterial = Material.matchMaterial(material);
                if (itemMaterial != null) mainItems[i] = new ItemStack(itemMaterial, amount, damage);
            }

            // Load armor items
            JsonArray armorItemsArray = root.getAsJsonArray("armor");
            ItemStack[] armorItems = new ItemStack[4]; // Assuming 4 armor slots
            for (int i = 0; i < armorItemsArray.size(); i++)
            {
                JsonObject jsonItem = armorItemsArray.get(i).getAsJsonObject();
                String material = jsonItem.get("material").getAsString();
                int amount = jsonItem.get("amount").getAsInt();
                short damage = jsonItem.has("damage") ? jsonItem.get("damage").getAsShort() : 0;

                Material itemMaterial = Material.matchMaterial(material);
                if (itemMaterial != null) armorItems[i] = new ItemStack(itemMaterial, amount, damage);
            }

            // Set inventory and armor contents
            player.getInventory().setContents(mainItems);
            player.getInventory().setArmorContents(armorItems);

            // Delete the file to prevent duplication
            // The file will be created again when the player next logs in.
            if (targetFile.exists())
                if (!targetFile.delete())
                    Runtime.getRuntime().exec("rm -f " + targetFile.getAbsolutePath()); //TODO: probably a really bad idea but its okay for now as the input isn't user controlled.
        } catch (IOException e) {
            e.printStackTrace(System.err);
        } catch (JsonParseException e) {
            System.err.println("[OSAS] Failed to parse inventory file for: " + player.getName());
            e.printStackTrace(System.err);
        }
    }

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
    
    private static Random random = new Random();
    public static String[] hash(String input)
    {
        try
        {
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            KeySpec spec = new PBEKeySpec(input.toCharArray(), salt, 65536, 128);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = f.generateSecret(spec).getEncoded();
            Base64.Encoder enc = Base64.getEncoder();
            return new String[] { enc.encodeToString(hash), enc.encodeToString(salt) };
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public static String hash(String input, String salt)
    {
        try
        {
            Base64.Decoder dec = Base64.getDecoder();
            KeySpec spec = new PBEKeySpec(input.toCharArray(), dec.decode(salt), 65536, 128);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = f.generateSecret(spec).getEncoded();
            Base64.Encoder enc = Base64.getEncoder();
            return enc.encodeToString(hash);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public static String sha256(String base)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++)
            {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }
}
