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

    private DreamsManager manager;
    private DreamsCommand commandmanager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.manager = new DreamsManager(this);
        this.commandmanager = new DreamsCommand(this);

        getCommand("dreams").setExecutor(commandmanager);
        getLogger().info("DreamsPlugin ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DreamsPlugin desativado.");
    }

    public DreamsManager getManager() {
        return manager;
    }

}
