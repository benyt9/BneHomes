package bne.bneplugins.bnehomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class BneHomes extends JavaPlugin {

    private Connection connection;

    @Override
    public void onEnable() {
        getLogger().info("§aBneHomes wurde aktiviert!");

        // Datenbank initialisieren
        initDatabase();
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                getLogger().info("§cDatenbank geschlossen.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initDatabase() {
        try {
            File dbFile = new File(getDataFolder(), "homes.db");
            if (!getDataFolder().exists()) getDataFolder().mkdirs();

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS homes (" +
                        "player TEXT," +
                        "name TEXT," +
                        "world TEXT," +
                        "x DOUBLE," +
                        "y DOUBLE," +
                        "z DOUBLE," +
                        "yaw FLOAT," +
                        "pitch FLOAT" +
                        ")");
            }
            getLogger().info("§aDatenbank initialisiert.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveHome(Player player, String name) {
        Location loc = player.getLocation();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO homes (player, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, name.toLowerCase());
            ps.setString(3, loc.getWorld().getName());
            ps.setDouble(4, loc.getX());
            ps.setDouble(5, loc.getY());
            ps.setDouble(6, loc.getZ());
            ps.setFloat(7, loc.getYaw());
            ps.setFloat(8, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Location getHome(Player player, String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM homes WHERE player = ? AND name = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, name.toLowerCase());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Location(
                        Bukkit.getWorld(rs.getString("world")),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void deleteHome(Player player, String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM homes WHERE player = ? AND name = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, name.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void listHomes(Player player) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM homes WHERE player = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();

            player.sendMessage("§eDeine Homes:");
            while (rs.next()) {
                player.sendMessage(" §7- §a" + rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl nutzen.");
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "sethome":
                if (args.length == 0) {
                    player.sendMessage("§cBenutze: /sethome <Name>");
                    return true;
                }
                saveHome(player, args[0]);
                player.sendMessage("§aHome §e" + args[0] + " §agesetzt!");
                break;

            case "delhome":
                if (args.length == 0) {
                    player.sendMessage("§cBenutze: /delhome <Name>");
                    return true;
                }
                deleteHome(player, args[0]);
                player.sendMessage("§cHome §e" + args[0] + " §cwurde gelöscht!");
                break;

            case "home":
                if (args.length == 0) {
                    player.sendMessage("§cBenutze: /home <Name>");
                    return true;
                }
                Location loc = getHome(player, args[0]);
                if (loc == null) {
                    player.sendMessage("§cDieses Home existiert nicht!");
                    return true;
                }
                player.teleport(loc);
                player.sendMessage("§aTeleportiere zu Home §e" + args[0] + "§a!");
                break;

            case "homes":
                listHomes(player);
                break;
        }
        return true;
    }
}
