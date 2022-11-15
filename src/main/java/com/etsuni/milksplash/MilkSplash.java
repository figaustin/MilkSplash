package com.etsuni.milksplash;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public final class MilkSplash extends JavaPlugin {


    FileConfiguration config = getConfig();
    private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy econ = null;
    private static Permission perms = null;
    private static Chat chat = null;

    @Override
    public void onEnable() {
        config.addDefault("item_name", "&fMilk Bottle");
        config.addDefault("lore", bottleLore());
        config.addDefault("cooldowns_enabled", false);
        config.addDefault("cooldown_time", -1);
        config.addDefault("cooldown_message", "&fYou can not do that for %seconds% more seconds!");
        config.addDefault("permissions_enabled", false);
        config.addDefault("no_permissions_throw_msg", "&fYou can not throw milk bottles!");
        config.addDefault("no_permissions_craft_msg", "&fYou can not craft milk bottles!");
        config.addDefault("only_cleanse_throwers_effects", true);
        config.addDefault("negative_effects_only", false);
        config.options().copyDefaults(true);
        saveConfig();


        MilkPotion milkPotion = new MilkPotion();
        Bukkit.addRecipe(milkPotion.createRecipe());

        this.getServer().getPluginManager().registerEvents(new MilkPotion(), this);
        this.getCommand("milk").setExecutor(new Commands());

        setupPermissions();
    }

    @Override
    public void onDisable() {
        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
    }


    public List<String> bottleLore() {
        List<String> stringList = new ArrayList<>();

        stringList.add("&fCleanses all potion effects");

        return stringList;
    }


    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static Permission getPermissions() {
        return perms;
    }

    public static Chat getChat() {
        return chat;
    }

}
