package net.oldschoolminecraft.osas;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Random;

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
