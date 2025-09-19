package bne.bneplugins.bnehomes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final JavaPlugin plugin;
    private File messagesFile;
    private FileConfiguration messagesConfig;
    private final MiniMessage miniMessage;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        setupMessagesFile();
        loadMessages();
    }

    private void setupMessagesFile() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    public void loadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Sendet eine Nachricht an einen Spieler mit Platzhaltern
     */
    public void send(Player player, String key, Map<String, String> placeholders) {
        if (!messagesConfig.contains(key)) return;

        String msg = messagesConfig.getString(key, key);

        // Platzhalter ersetzen
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        // %prefix% automatisch ersetzen
        msg = msg.replace("%prefix%", messagesConfig.getString("prefix", ""));

        Component component = miniMessage.deserialize(msg);
        player.sendMessage(component);
    }

    /**
     * Sendet eine Nachricht ohne Platzhalter
     */
    public void send(Player player, String key) {
        send(player, key, new HashMap<>());
    }

    /**
     * Sendet eine Nachricht an die Konsole mit Platzhaltern
     */
    public void sendConsole(String key, Map<String, String> placeholders) {
        if (!messagesConfig.contains(key)) return;

        String msg = messagesConfig.getString(key, key);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        msg = msg.replace("%prefix%", messagesConfig.getString("prefix", ""));
        // Minimessage-Tags für Konsole entfernen
        String plain = miniMessage.stripTags(msg);
        plugin.getServer().getConsoleSender().sendMessage(plain);
    }

    /**
     * Sendet eine Nachricht an die Konsole ohne Platzhalter
     */
    public void sendConsole(String key) {
        sendConsole(key, new HashMap<>());
    }

    /**
     * Loggt eine Nachricht in der Konsole (alias)
     */
    public void log(String key, Map<String, String> placeholders) {
        sendConsole(key, placeholders);
    }

    /**
     * Lädt die messages.yml neu (Reload)
     */
    public void reload() {
        loadMessages();
    }

    /**
     * Gibt rohen String zurück
     */
    public String getRaw(String key) {
        return messagesConfig.getString(key, key);
    }
}
