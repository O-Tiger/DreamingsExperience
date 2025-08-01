package com.Tiger.dreamings;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DreamsPlugin - A Minecraft plugin that adds dream and nightmare mechanics.
 * Players can experience dreams or nightmares when entering a bed.
 * 
 * @author __Tiger
 */

public class DreamsPlugin extends JavaPlugin implements Listener {

    private DreamsManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new DreamsManager(this);
        getLogger().info("DreamsPlugin ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DreamsPlugin desativado.");
    }

    public DreamsManager getManager() {
        return manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return manager.handleCommand(sender, command, label, args);
    }

    }
