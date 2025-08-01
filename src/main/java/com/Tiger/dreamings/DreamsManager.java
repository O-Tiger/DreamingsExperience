package com.Tiger.dreamings;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DreamsManager implements Listener {
    private enum DreamType {
        DREAM, NIGHTMARE
    }

    private final JavaPlugin plugin;

    private final Random random = new Random();

    // MAPS

    private final Map<UUID, Boolean> endingDream = new HashMap<>();
    private final Map<UUID, DreamType> activeDreams = new HashMap<>();

    // Para rastrear a entidade que controla a câmera de cada jogador
    private final Map<UUID, ArmorStand> cameraEntities = new HashMap<>();

    // Para guardar modo de jogo original e localização real do jogador antes do
    // sonho
    private final Map<UUID, GameMode> originalGameModes = new HashMap<>();
    private final Map<UUID, Location> originalLocations = new HashMap<>();

    private FileConfiguration lang;
    private Location dreamLocation;
    private Location nightmareLocation;
    private Location localPos;

    private String getRandomMessage(String path) {
        var section = lang.get(path);
        if (section instanceof java.util.List<?> list && !list.isEmpty()) {
            Object chosen = list.get(random.nextInt(list.size()));
            return chosen instanceof String ? (String) chosen : null;
        } else if (section instanceof String single) {
            return single;
        }
        return null;
    }

    private void loadLang() {
        String langCode = plugin.getConfig().getString("lang", "pt_br");
        File langFile = new File(plugin.getDataFolder(), "lang/" + langCode + ".yml");

        if (!langFile.exists()) {
            plugin.saveResource("lang/" + langCode + ".yml", false);
        }

        lang = YamlConfiguration.loadConfiguration(langFile);
    }

    private void loadLocations() {
        nightmareLocation = getLocationFromConfig("dimension.nightmare");
        dreamLocation = getLocationFromConfig("dimension.dreams");
    }

    private Location getLocationFromConfig(String path) {
        FileConfiguration config = plugin.getConfig();
        String worldName = config.getString(path + ".world");
        if (worldName == null || worldName.isEmpty()) {
            plugin.getLogger().warning("[DreamsPlugin] Mundo não definido para " + path);
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[DreamsPlugin] Mundo não encontrado: " + worldName);
            return null;
        }

        try {
            double x = config.getDouble(path + ".x");
            double y = config.getDouble(path + ".y");
            double z = config.getDouble(path + ".z");
            float yaw = (float) config.getDouble(path + ".yaw");
            float pitch = (float) config.getDouble(path + ".pitch");
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("[DreamsPlugin] Coordenadas inválidas para " + path + ": " + e.getMessage());
            return null;
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();

        double posX = player.getLocation().getX();
        double posY = player.getLocation().getY();
        double posZ = player.getLocation().getZ();
        float posPitch = player.getLocation().getPitch();
        float posYaw = player.getLocation().getYaw();
        World posWorld = player.getLocation().getWorld();
        localPos = new Location(posWorld, posX, posY, posZ, posYaw, posPitch);
       
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK)
            return;

        if (!plugin.getConfig().getBoolean("dimension.enabled", true)) {
            sendMessage(event.getPlayer(), lang.getString("messages.disabled"));
            return;
        }

        // Se já estiver "sonhando", não tenta de novo
        if (cameraEntities.containsKey(player.getUniqueId()))
            return;

        int dreamTriggerChance = plugin.getConfig().getInt("dream_trigger_chance", 25);
        if (random.nextInt(100) < dreamTriggerChance) {
            int nightmareChance = plugin.getConfig().getInt("dimension.nightmare.chance", 60);
            if (random.nextInt(100) < nightmareChance && nightmareLocation != null) {
                triggerNightmare(player);
            } else if (dreamLocation != null) {
                triggerDream(player);
            }
        }
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (cameraEntities.containsKey(uuid)) {
            // Previnir loop recursivo
            if (endingDream.getOrDefault(uuid, false))
                return;
            endingDream.put(uuid, true);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                endDreamVision(player);
                endingDream.remove(uuid);
            }, 5L); // Executar no tick seguinte
        }

    }

    private void triggerNightmare(Player player) {

        sendMessage(player, lang.getString("messages.nightmare.init"));
        startDreamVision(player, nightmareLocation);
        activeDreams.put(player.getUniqueId(), DreamType.NIGHTMARE);

        // Aplica efeitos pesadelo
        int duration = plugin.getConfig().getInt("dimension.nightmare.duration", 150);
        List<String> effects = plugin.getConfig().getStringList("dimension.nightmare.effects");

        for (String effectStr : effects) {
            String[] parts = effectStr.split(":");
            PotionEffectType type = PotionEffectType.getByName(parts[0]);
            int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (type != null) {
                player.addPotionEffect(new PotionEffect(type, duration, amplifier));
            }
        }
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!cameraEntities.containsKey(player.getUniqueId())) {
                    cancel(); // jogador acordou
                    return;
                }

                // Após 5 segundos, mostra 'middle', depois 'scream'
                ticks += 20;
                if (ticks == 40) {
                    sendMessage(player, getRandomMessage("messages.nightmare.middle"));
                } else if (ticks == 80) {
                    sendMessage(player, getRandomMessage("messages.nightmare.scream"));
                    cancel(); // depois disso, para
                }
            }
        }.runTaskTimer(plugin, 20L, 40L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (cameraEntities.containsKey(player.getUniqueId())) {
                    endDreamVision(player);
                }
            }
        }.runTaskLater(plugin, duration);
    }

    private void triggerDream(Player player) {
        sendMessage(player, lang.getString("messages.dream.init"));
        startDreamVision(player, dreamLocation);
        activeDreams.put(player.getUniqueId(), DreamType.DREAM);

        int duration = plugin.getConfig().getInt("dimension.dreams.duration", 150);
        List<String> effects = plugin.getConfig().getStringList("dimension.dreams.effects");

        for (String effectStr : effects) {
            String[] parts = effectStr.split(":");
            PotionEffectType type = PotionEffectType.getByName(parts[0]);
            int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (type != null) {
                player.addPotionEffect(new PotionEffect(type, duration, amplifier));
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (cameraEntities.containsKey(player.getUniqueId())) {
                    endDreamVision(player);
                }
            }
        }.runTaskLater(plugin, duration);
    }

    private void startDreamVision(Player player, Location loc) {
        player.getPlayer();
        UUID uuid = player.getUniqueId();
        // Cria entidade para controlar a câmera
        ArmorStand camera = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        camera.setVisible(false);
        camera.setInvulnerable(true);
        camera.setMarker(true);
        camera.setGravity(false);
        camera.setCollidable(false);

        cameraEntities.put(uuid, camera);
        player.setGameMode(GameMode.SPECTATOR);
        player.setSpectatorTarget(camera);
    }

    private void endDreamVision(Player player) {

        player.getPlayer();
        UUID uuid = player.getUniqueId();
        DreamType type = activeDreams.getOrDefault(uuid, DreamType.DREAM);
        String typeDream = switch (type) {
            case NIGHTMARE -> "messages.nightmare.end";
            case DREAM -> "messages.dream.end";
            default -> "messages.awake";
        };
        if (player.isOnline() && player.getGameMode() == GameMode.SPECTATOR) {

            // Remove efeitos aplicados, entidade da câmera e dados armazenados
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.CONFUSION);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.removePotionEffect(PotionEffectType.SLOW);

            ArmorStand camera = cameraEntities.get(uuid);
            if (camera != null && !camera.isDead()) {
                camera.remove();
            }

            cameraEntities.remove(uuid);
            originalLocations.remove(uuid);

            GameMode gmFinal = originalGameModes.getOrDefault(uuid, GameMode.SURVIVAL);
            player.setGameMode(gmFinal);
            if (localPos != null)
                player.teleport(localPos);

        }
        originalGameModes.remove(uuid);
        sendMessage(player, getRandomMessage(typeDream));

    }

    private void sendMessage(Player player, String rawMessage) {
        if (rawMessage == null)
            return;
        String msg = ChatColor.translateAlternateColorCodes('&', parse(rawMessage, player));
        player.sendMessage(msg);
    }

    private String parse(String message, Player player) {
        return message
                .replace("%player%", player.getName())
                .replace("%world%", player.getWorld().getName())
                .replace("%health%", String.valueOf((int) player.getHealth()));
    }

    public boolean handleCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return true;
        }

        if (!player.hasPermission("dreams.set")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão.");
            return true;
        }
        if (!player.hasPermission("dreams.reload")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão.");
            return true;
        }

        Location loc = player.getLocation();
        String basePath = switch (label.toLowerCase()) {
            case "setdream" -> "dimension.dreams";
            case "setnightmare" -> "dimension.nightmare";
            default -> null;
        };

        if (basePath == null)
            return false;

        FileConfiguration config = plugin.getConfig();
        config.set(basePath + ".world", loc.getWorld().getName());
        config.set(basePath + ".x", loc.getX());
        config.set(basePath + ".y", loc.getY());
        config.set(basePath + ".z", loc.getZ());
        config.set(basePath + ".yaw", loc.getYaw());
        config.set(basePath + ".pitch", loc.getPitch());
        plugin.saveConfig();

        loadLocations(); // recarrega

        player.sendMessage(ChatColor.GREEN + "Local de " + label.replace("set", "") + " salvo!");
        return true;

    }

    // BUILDER

    public DreamsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadLang();
        loadLocations();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
}
