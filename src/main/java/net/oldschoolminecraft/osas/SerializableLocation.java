package net.oldschoolminecraft.osas;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class SerializableLocation
{
    private String world;
    public double x, y, z;
    public float yaw, pitch;

    public SerializableLocation() {}

    public SerializableLocation fromBukkitLocation(Location loc)
    {
        this.world = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
        return this;
    }

    public Location getBukkitLocation()
    {
        return new Location(Bukkit.getServer().getWorld(world), x, y, z, yaw, pitch);
    }
}
