package bne.bneplugins.bnehomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.sql.*;

public class BneHomes extends JavaPlugin {

    private Connection connection;
    private FileConfiguration config;
    private FileConfiguration messages;
    private String prefix;

    @Override
    public void onEnable() {
        // Configs laden oder erstellen
        saveDefaultConfig();
        config = getConfig();
        saveResource("messages.yml", false);
        messages = getConfig("messages.yml");
        prefix = config.getString("prefix");

        getLogger().info(prefix + "§aBneHomes wurde aktiviert!");

        // Datenbank initialisieren
        initDatabase();

        // BungeeCord Channel registrieren, falls aktiviert
        if(config.getBoolean("bungeecord-enabled", true))
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                getLogger().info(prefix + "§cDatenbank geschlossen.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initDatabase() {
        try {
            File dbFile = new File(getDataFolder(), config.getString("database-file", "homes.db"));
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
                        "pitch FLOAT," +
                        "server TEXT" +
                        ")");
            }
            getLogger().info(prefix + "§aDatenbank initialisiert.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveHome(Player player, String name) {
        Location loc = player.getLocation();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO homes (player, name, world, x, y, z, yaw, pitch, server) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, name.toLowerCase());
            ps.setString(3, loc.getWorld().getName());
            ps.setDouble(4, loc.getX());
            ps.setDouble(5, loc.getY());
            ps.setDouble(6, loc.getZ());
            ps.setFloat(7, loc.getYaw());
            ps.setFloat(8, loc.getPitch());
            ps.setString(9, config.getString("current-server"));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Home getHome(Player player, String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM homes WHERE player = ? AND name = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, name.toLowerCase());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Home(
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch"),
                        rs.getString("server")
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
                "SELECT name, server FROM homes WHERE player = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();

            player.sendMessage(prefix + "§eDeine Homes:");
            while (rs.next()) {
                String homeName = rs.getString("name");
                String server = rs.getString("server");
                player.sendMessage(" §7- §a" + homeName + " §7(" + server + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void connectToServer(Player player, String server) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(this, "BungeeCord", b.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(prefix + "§cFehler beim Verbinden zum Server!");
        }
    }

    private String getMessage(String key, String... placeholders) {
        String msg = messages.getString(key, key);
        for (String placeholder : placeholders) {
            String[] split = placeholder.split(";", 2);
            if(split.length==2) msg = msg.replace(split[0], split[1]);
        }
        return msg.replace("%prefix%", prefix);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getMessage("only-players"));
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "sethome":
                if (args.length == 0) {
                    player.sendMessage(getMessage("usage-sethome"));
                    return true;
                }
                saveHome(player, args[0]);
                player.sendMessage(getMessage("sethome-success", "%home%;" + args[0]));
                break;

            case "delhome":
                if (args.length == 0) {
                    player.sendMessage(getMessage("usage-delhome"));
                    return true;
                }
                deleteHome(player, args[0]);
                player.sendMessage(getMessage("delhome-success", "%home%;" + args[0]));
                break;

            case "home":
                if (args.length == 0) {
                    player.sendMessage(getMessage("usage-home"));
                    return true;
                }
                Home home = getHome(player, args[0]);
                if (home == null) {
                    player.sendMessage(getMessage("home-not-exist"));
                    return true;
                }
                if (home.server.equalsIgnoreCase(config.getString("current-server"))) {
                    player.teleport(home.toLocation(player.getServer().getWorld(home.world)));
                    player.sendMessage(getMessage("home-teleport-local", "%home%;" + args[0]));
                } else {
                    player.sendMessage(getMessage("home-teleport-remote", "%server%;" + home.server));
                    connectToServer(player, home.server);
                }
                break;

            case "homes":
                listHomes(player);
                break;
        }
        return true;
    }

    private static class Home {
        String world;
        double x, y, z;
        float yaw, pitch;
        String server;

        Home(String world, double x, double y, double z, float yaw, float pitch, String server) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.server = server;
        }

        Location toLocation(org.bukkit.World worldObj) {
            return new Location(worldObj, x, y, z, yaw, pitch);
        }
    }
}
