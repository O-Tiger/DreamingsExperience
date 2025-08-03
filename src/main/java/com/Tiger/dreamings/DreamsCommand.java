package com.Tiger.dreamings;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class DreamsCommand implements CommandExecutor {

    private final DreamsPlugin plugin;

    public DreamsCommand(DreamsPlugin plugin) {
        this.plugin = plugin;
    }

    private void reloadLang() {
        plugin.getManager().loadLang();
    }

    private void reloadLocations() {
        plugin.getManager().loadLocations();
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "===== DreamsPlugin Help =====");
        player.sendMessage(ChatColor.GOLD + "/dreams help" + ChatColor.WHITE + " - Mostra esta ajuda");
        player.sendMessage(ChatColor.GOLD + "/dreams reload" + ChatColor.WHITE
                + " - Recarrega a configuração (requires dreams.admin)");
        player.sendMessage(ChatColor.GOLD + "/setdream" + ChatColor.WHITE
                + " - Define sua posição como local do sonho (requires dreams.set)");
        player.sendMessage(ChatColor.GOLD + "/setnightmare" + ChatColor.WHITE
                + " - Define sua posição como local do pesadelo (requires dreams.set)");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ignorar console
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return true;
        }
        // Sem nenhum argumento ou help
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            sendHelp(player);
            return true;
        }
        String sub = args[0].toLowerCase();

        // Reload
        if (sub.equals("reload")) {
            if (!player.hasPermission("dreams.admin")) {
                player.sendMessage(ChatColor.RED + "Você não tem permissão para recarregar o plugin.");
                return true;
            }
            plugin.reloadConfig();
            reloadLang();
            reloadLocations();
            player.sendMessage(ChatColor.GREEN + "DreamsPlugin recarregado com sucesso.");
            return true;
        }

        boolean isSetDream = label.equalsIgnoreCase("setdream") || sub.equals("setdream");
        boolean isSetNightmare = label.equalsIgnoreCase("setnightmare") || sub.equals("setnightmare");

        if (isSetDream || isSetNightmare) {
            if (!player.hasPermission("dreams.set")) {
                player.sendMessage(ChatColor.RED + "Você não tem permissão para definir locais.");
                return true;
            }

            Location loc = player.getLocation();
            String basePath = isSetDream ? "dimension.dreams" : "dimension.nightmare";
            String niceName = isSetDream ? "sonho" : "pesadelo";

            FileConfiguration config = plugin.getConfig();
            config.set(basePath + ".world", loc.getWorld().getName());
            config.set(basePath + ".x", loc.getX());
            config.set(basePath + ".y", loc.getY());
            config.set(basePath + ".z", loc.getZ());
            config.set(basePath + ".yaw", loc.getYaw());
            config.set(basePath + ".pitch", loc.getPitch());
            plugin.saveConfig();

            reloadLocations();
            player.sendMessage(ChatColor.GREEN + "Local de " + niceName + " salvo!");
            return true;
        }
        player.sendMessage(ChatColor.RED + "Subcomando inválido. Use /dreams help para ver os disponíveis.");
        return true;
    }

}
