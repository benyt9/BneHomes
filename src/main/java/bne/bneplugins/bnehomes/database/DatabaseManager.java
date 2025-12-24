package bne.bneplugins.bnehomes.database;

import bne.bneplugins.bnehomes.BneHomes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private Connection connection;

    public void connect() throws SQLException {
        FileConfiguration config = BneHomes.getInstance().getConfig();
        String type = config.getString("database.type", "sqlite");

        if (type.equalsIgnoreCase("mysql")) {
            String host = config.getString("database.host");
            int port = config.getInt("database.port");
            String db = config.getString("database.name");
            String user = config.getString("database.user");
            String pass = config.getString("database.password");
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + db, user, pass);
        } else {
            File file = new File(BneHomes.getInstance().getDataFolder(), "homes.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        }

        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS player_homes (" +
                    "uuid VARCHAR(36), name VARCHAR(32), world VARCHAR(64), " +
                    "x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, " +
                    "PRIMARY KEY (uuid, name))");
        }
    }

    public void setHome(UUID uuid, String name, Location loc) {
        String sql = "REPLACE INTO player_homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name.toLowerCase());
            ps.setString(3, loc.getWorld().getName());
            ps.setDouble(4, loc.getX());
            ps.setDouble(5, loc.getY());
            ps.setDouble(6, loc.getZ());
            ps.setFloat(7, loc.getYaw());
            ps.setFloat(8, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public Location getHome(UUID uuid, String name) {
        String sql = "SELECT * FROM player_homes WHERE uuid = ? AND name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Location(
                        Bukkit.getWorld(rs.getString("world")),
                        rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch")
                );
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void deleteHome(UUID uuid, String name) {
        String sql = "DELETE FROM player_homes WHERE uuid = ? AND name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}