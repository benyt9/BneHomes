package bne.bneplugins.bnehomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class BneHomes extends JavaPlugin {

    private HomeManager homeManager;
    private boolean proxyEnabled;
    private String currentServer;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        messageManager = new MessageManager(this);
        currentServer = getConfig().getString("current-server", "lobby");

        String dbType = getConfig().getString("database.type", "SQLITE");
        proxyEnabled = getConfig().getBoolean("proxy-enabled", true) && dbType.equalsIgnoreCase("MYSQL");

        try {
            homeManager = new HomeManager();
            homeManager.init(this);
        } catch (SQLException e) {
            messageManager.log("db-error", new HashMap<>());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("proxy", String.valueOf(proxyEnabled));
        messageManager.log("plugin-enabled", placeholders);
    }

    @Override
    public void onDisable() {
        try {
            if(homeManager != null) homeManager.shutdown();
        } catch (SQLException e) {
            messageManager.log("db-error", new HashMap<>());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl nutzen!");
            return true;
        }

        try {
            switch (command.getName().toLowerCase()) {

                case "sethome": {
                    String homeName = args.length > 0 ? args[0] : "home";
                    Location loc = player.getLocation();
                    homeManager.saveHome(player, homeName, loc, currentServer);

                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("home", homeName);
                    messageManager.send(player, "sethome-success", placeholders);
                    break;
                }

                case "home": {
                    String homeName = args.length > 0 ? args[0] : "home";
                    Home home = homeManager.getHome(player, homeName);

                    if (home == null) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("home", homeName);
                        messageManager.send(player, "home-not-found",placeholders);
                        return true;
                    }

                    if (home.getServer().equals(currentServer) || !proxyEnabled) {
                        Location target = new Location(
                                Bukkit.getWorld(home.getWorld()),
                                home.getX(), home.getY(), home.getZ(),
                                home.getYaw(), home.getPitch()
                        );
                        player.teleport(target);

                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("home", homeName);
                        messageManager.send(player, "home-teleport", placeholders);

                    } else {
                        homeManager.sendProxyTeleport(player, home.getServer());
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("home", homeName);
                        messageManager.send(player, "home-proxy-teleport", placeholders);
                    }
                    break;
                }

                case "delhome": {
                    String homeName = args.length > 0 ? args[0] : "home";
                    homeManager.deleteHome(player, homeName);

                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("home", homeName);
                    messageManager.send(player, "delhome-success", placeholders);
                    break;
                }

                case "homes": {
                    messageManager.send(player, "homes-list-title");

                    for (Home h : homeManager.getHomes(player)) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("home", h.getName());
                        placeholders.put("server", h.getServer());
                        messageManager.send(player, "homes-list-entry", placeholders);
                    }
                    break;
                }

                default:
                    return false;
            }
        } catch (SQLException e) {
            messageManager.send(player, "db-error");
            e.printStackTrace();
        }

        return true;
    }
}
