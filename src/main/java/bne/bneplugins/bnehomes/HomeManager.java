package bne.bneplugins.bnehomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HomeManager {

    private HomeDatabase database;
    private BneHomes plugin;

    public void init(BneHomes plugin) throws SQLException {
        this.plugin = plugin;
        String dbType = plugin.getConfig().getString("database.type", "SQLITE");
        if(dbType.equalsIgnoreCase("MYSQL")) {
            String host = plugin.getConfig().getString("database.mysql.host");
            int port = plugin.getConfig().getInt("database.mysql.port");
            String db = plugin.getConfig().getString("database.mysql.database");
            String user = plugin.getConfig().getString("database.mysql.username");
            String pass = plugin.getConfig().getString("database.mysql.password");
            database = new MySQLDatabase(host, port, db, user, pass);
        } else {
            String file = plugin.getConfig().getString("database.sqlite-file", "homes.db");
            database = new SQLiteDatabase(file);
        }
        database.connect();
    }

    public void shutdown() throws SQLException { database.disconnect(); }

    public void saveHome(Player player, String name, Location loc, String server) throws SQLException {
        Home home = new Home(player.getUniqueId(), name, loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), server);
        database.saveHome(home);
    }

    public Home getHome(Player player, String name) throws SQLException {
        return database.getHome(player.getUniqueId(), name);
    }

    public void deleteHome(Player player, String name) throws SQLException {
        database.deleteHome(player.getUniqueId(), name);
    }

    public List<Home> getHomes(Player player) throws SQLException {
        List<Home> list = new ArrayList<>();
        for(String name : database.getAllHomeNames(player.getUniqueId())) {
            Home h = database.getHome(player.getUniqueId(), name);
            if(h != null) list.add(h);
        }
        return list;
    }

    public void sendProxyTeleport(Player player, String server) {
        if(!plugin.getConfig().getString("database.type").equalsIgnoreCase("MYSQL")) return;
        try {
            player.sendPluginMessage(plugin, "BungeeCord", buildTeleportMessage(server));
        } catch(Exception e) {
            plugin.getLogger().warning("Fehler beim Proxy-Teleport: " + e.getMessage());
        }
    }

    private byte[] buildTeleportMessage(String server) {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(b)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (Exception ignored) {}
        return b.toByteArray();
    }
}
