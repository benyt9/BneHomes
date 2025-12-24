package bne.bneplugins.bnehomes;

import bne.bneplugins.bnehomes.commands.HomeCommand;
import bne.bneplugins.bnehomes.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;

public class BneHomes extends JavaPlugin {

    private static BneHomes instance;
    private DatabaseManager databaseManager;
    private FileConfiguration messagesConfig;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        createMessagesConfig();

        this.databaseManager = new DatabaseManager();
        try {
            databaseManager.connect();
            getLogger().info("Datenbank erfolgreich verbunden!");
        } catch (SQLException e) {
            getLogger().severe("Datenbank-Fehler! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        HomeCommand homeCommand = new HomeCommand();
        getCommand("sethome").setExecutor(homeCommand);
        getCommand("home").setExecutor(homeCommand);
        getCommand("delhome").setExecutor(homeCommand);
    }

    private void createMessagesConfig() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) saveResource("messages.yml", false);
        messagesConfig = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Sendet eine MiniMessage-Nachricht an einen Spieler.
     * @param player Der Empfänger
     * @param path Der Pfad in der messages.yml
     * @param replacements Paare von Platzhaltern und Werten, z.B. "%name%", "Home1"
     */
    public void sendMessage(Player player, String path, String... replacements) {
        String message = messagesConfig.getString(path, "Path not found: " + path);
        String prefix = messagesConfig.getString("prefix", "");
        String fullRawMessage = prefix + message;

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                fullRawMessage = fullRawMessage.replace(replacements[i], replacements[i + 1]);
            }
        }

        Component parsed = miniMessage.deserialize(fullRawMessage);
        player.sendMessage(parsed);
    }

    public static BneHomes getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
}