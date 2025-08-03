package com.Tiger.dreamings;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;


public class DreamsCommand implements CommandExecutor {

    private final DreamsPlugin plugin;

    public DreamsCommand(DreamsPlugin plugin) {
        this.plugin = plugin;
    }

    private void loadLang() {
        plugin.getManager().loadLang(); 
    }

    private void loadLocations() {
        plugin.getManager().loadLocations(); 
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("dreams.admin")) {
                player.sendMessage(ChatColor.RED + "Você não tem permissão para recarregar o plugin.");
                return true;
            }

            plugin.reloadConfig();
            loadLang();
            loadLocations();
            player.sendMessage(ChatColor.GREEN + "DreamsPlugin recarregado com sucesso.");
            return true;
        }

        if (!player.hasPermission("dreams.set")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para definir locais.");
            return true;
        }

        Location loc = player.getLocation();
        String basePath = switch (label.toLowerCase()) {
            case "setdream" -> "dimension.dreams";
            case "setnightmare" -> "dimension.nightmare";
            default -> null;
        };

        if (basePath == null) return false;

        FileConfiguration config = plugin.getConfig();
        config.set(basePath + ".world", loc.getWorld().getName());
        config.set(basePath + ".x", loc.getX());
        config.set(basePath + ".y", loc.getY());
        config.set(basePath + ".z", loc.getZ());
        config.set(basePath + ".yaw", loc.getYaw());
        config.set(basePath + ".pitch", loc.getPitch());
        plugin.saveConfig();

        loadLocations();
        player.sendMessage(ChatColor.GREEN + "Local de " + label.replace("set", "") + " salvo!");
        return true;
    }
    

}
