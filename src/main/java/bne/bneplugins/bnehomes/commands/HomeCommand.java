package bne.bneplugins.bnehomes.commands;

import bne.bneplugins.bnehomes.BneHomes;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        BneHomes plugin = BneHomes.getInstance();

        if (cmd.getName().equalsIgnoreCase("sethome")) {
            if (!player.hasPermission("bnehomes.set")) {
                plugin.sendMessage(player, "no_permission");
                return true;
            }
            if (args.length != 1) {
                player.sendMessage("§cNutze: /sethome <Name>");
                return true;
            }
            plugin.getDatabaseManager().setHome(player.getUniqueId(), args[0], player.getLocation());
            plugin.sendMessage(player, "home_set", "%name%", args[0]);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("home")) {
            if (!player.hasPermission("bnehomes.teleport")) {
                plugin.sendMessage(player, "no_permission");
                return true;
            }
            if (args.length != 1) {
                player.sendMessage("§cNutze: /home <Name>");
                return true;
            }
            Location loc = plugin.getDatabaseManager().getHome(player.getUniqueId(), args[0]);
            if (loc == null) {
                plugin.sendMessage(player, "home_not_found");
                return true;
            }
            player.teleport(loc);
            plugin.sendMessage(player, "home_teleport", "%name%", args[0]);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("delhome")) {
            if (!player.hasPermission("bnehomes.delete")) {
                plugin.sendMessage(player, "no_permission");
                return true;
            }
            if (args.length != 1) {
                player.sendMessage("§cNutze: /delhome <Name>");
                return true;
            }
            plugin.getDatabaseManager().deleteHome(player.getUniqueId(), args[0]);
            plugin.sendMessage(player, "home_deleted", "%name%", args[0]);
            return true;
        }

        return false;
    }
}