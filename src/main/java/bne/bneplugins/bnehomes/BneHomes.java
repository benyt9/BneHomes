package bne.bneplugins.bnehomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public class BneHomes extends JavaPlugin {

    private HomeManager homeManager;
    private boolean proxyEnabled;
    private String prefix;
    private String currentServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        prefix = getConfig().getString("prefix", "§7[§eBneHomes§7] §r§7");
        currentServer = getConfig().getString("current-server", "lobby");

        // Proxy nur aktiv, wenn MySQL
        String dbType = getConfig().getString("database.type", "SQLITE");
        proxyEnabled = getConfig().getBoolean("proxy-enabled", true) && dbType.equalsIgnoreCase("MYSQL");

        try {
            homeManager = new HomeManager();
            homeManager.init(this);
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Fehler beim Initialisieren der Datenbank", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info(prefix + "BneHomes aktiviert! Proxy-Support: " + proxyEnabled);
    }

    @Override
    public void onDisable() {
        try {
            if(homeManager != null) homeManager.shutdown();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Fehler beim Schließen der Datenbank", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "Dieser Befehl kann nur von Spielern genutzt werden!");
            return true;
        }

        try {
            switch(command.getName().toLowerCase()) {
                case "sethome":
                    String homeName = args.length > 0 ? args[0] : "home";
                    Location loc = player.getLocation();
                    homeManager.saveHome(player, homeName, loc, currentServer);
                    player.sendMessage(prefix + "Home §b" + homeName + " §7gesetzt!");
                    break;

                case "home":
                    homeName = args.length > 0 ? args[0] : "home";
                    Home home = homeManager.getHome(player, homeName);
                    if(home == null) {
                        player.sendMessage(prefix + "Home §b" + homeName + " §7nicht gefunden!");
                        return true;
                    }

                    if(home.getServer().equals(currentServer) || !proxyEnabled) {
                        Location target = new Location(
                                Bukkit.getWorld(home.getWorld()),
                                home.getX(), home.getY(), home.getZ(),
                                home.getYaw(), home.getPitch()
                        );
                        player.teleport(target);
                        player.sendMessage(prefix + "Teleportiere zu §b" + homeName + "§7...");
                    } else {
                        homeManager.sendProxyTeleport(player, home.getServer());
                        player.sendMessage(prefix + "Teleportiere zu §b" + homeName + "§7 via Proxy...");
                    }
                    break;

                case "delhome":
                    homeName = args.length > 0 ? args[0] : "home";
                    homeManager.deleteHome(player, homeName);
                    player.sendMessage(prefix + "Home §b" + homeName + " §7gelöscht!");
                    break;

                case "homes":
                    player.sendMessage(prefix + "Homes:");
                    for(Home h : homeManager.getHomes(player)) {
                        player.sendMessage("§b- " + h.getName() + " (§7Server: " + h.getServer() + "§7)");
                    }
                    break;

                default:
                    return false;
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Fehler bei Home-Befehl", e);
            player.sendMessage(prefix + "Fehler beim Verarbeiten des Befehls!");
        }

        return true;
    }
}
