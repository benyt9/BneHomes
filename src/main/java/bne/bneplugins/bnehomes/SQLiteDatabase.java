package bne.bneplugins.bnehomes;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SQLiteDatabase implements HomeDatabase {

    private Connection connection;
    private final String filePath;

    public SQLiteDatabase(String filePath) { this.filePath = filePath; }

    @Override
    public void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + filePath);
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS homes (" +
                    "uuid TEXT, name TEXT, world TEXT, x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, server TEXT, PRIMARY KEY(uuid,name))");
        }
    }

    @Override
    public void disconnect() throws SQLException {
        if(connection != null) connection.close();
    }

    @Override
    public void saveHome(Home home) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "REPLACE INTO homes(uuid,name,world,x,y,z,yaw,pitch,server) VALUES(?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, home.getOwner().toString());
            ps.setString(2, home.getName());
            ps.setString(3, home.getWorld());
            ps.setDouble(4, home.getX());
            ps.setDouble(5, home.getY());
            ps.setDouble(6, home.getZ());
            ps.setFloat(7, home.getYaw());
            ps.setFloat(8, home.getPitch());
            ps.setString(9, home.getServer());
            ps.executeUpdate();
        }
    }

    @Override
    public Home getHome(UUID player, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM homes WHERE uuid=? AND name=?")) {
            ps.setString(1, player.toString());
            ps.setString(2, name);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return new Home(player,
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch"),
                        rs.getString("server"));
            }
        }
        return null;
    }

    @Override
    public void deleteHome(UUID player, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM homes WHERE uuid=? AND name=?")) {
            ps.setString(1, player.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }

    @Override
    public List<String> getAllHomeNames(UUID player) throws SQLException {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM homes WHERE uuid=?")) {
            ps.setString(1, player.toString());
            ResultSet rs = ps.executeQuery();
            while(rs.next()) list.add(rs.getString("name"));
        }
        return list;
    }
}
