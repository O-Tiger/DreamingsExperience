package com.Tiger.dreamings;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DreamsPlugin - A Minecraft plugin that adds dream and nightmare mechanics.
 * Players can experience dreams or nightmares when entering a bed.
 * 
 * @author __Tiger
 */

public class DreamsPlugin extends JavaPlugin implements Listener {

    private DreamsCommand CommandManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        CommandManager = new DreamsCommand(this);

        getCommand("dreams").setExecutor(new DreamsCommand(this));
        getCommand("setdream").setExecutor(new DreamsCommand(this));
        getCommand("setnightmare").setExecutor(new DreamsCommand(this));

        getLogger().info("DreamsPlugin ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DreamsPlugin desativado.");
    }

    public DreamsCommand getManager() {
        return CommandManager;
    }

}
