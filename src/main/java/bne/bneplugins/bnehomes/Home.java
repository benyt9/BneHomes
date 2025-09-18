package bne.bneplugins.bnehomes;

import java.util.UUID;

public class Home {
    private final UUID owner;
    private final String name;
    private final String world;
    private final double x, y, z;
    private final float yaw, pitch;
    private final String server;

    public Home(UUID owner, String name, String world, double x, double y, double z, float yaw, float pitch, String server) {
        this.owner = owner;
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.server = server;
    }

    public UUID getOwner() { return owner; }
    public String getName() { return name; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public String getServer() { return server; }
}
