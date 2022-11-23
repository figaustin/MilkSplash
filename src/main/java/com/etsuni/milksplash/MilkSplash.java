package com.etsuni.milksplash;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;

import org.bukkit.Material;
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

    public static String VERSION;

    protected static MilkSplash plugin;

    @Override
    public void onEnable() {
        plugin = this;
        VERSION = this.getServer().getVersion();

        config.addDefault("regular_potion_name", "&fMilk Bottle");
        config.addDefault("splash_potion_name", "&fMilk Splash");
        config.addDefault("regular_potion_lore", bottleLore());
        config.addDefault("splash_potion_lore", splashLore());
        config.addDefault("cooldowns_enabled", false);
        config.addDefault("cooldown_time", -1);
        config.addDefault("cooldown_message", "&fYou can not do that for %seconds% more seconds!");
        config.addDefault("permissions_enabled", false);
        config.addDefault("crafting_enabled", false);
        config.addDefault("brewing_enabled", true);
        config.addDefault("no_permission_brew_msg", "&fYou can not brew milk bottles!");
        config.addDefault("no_permission_throw_msg", "&fYou can not throw milk bottles!");
        config.addDefault("no_permission_craft_msg", "&fYou can not craft milk bottles!");
        config.addDefault("only_cleanse_throwers_effects", true);
        config.addDefault("negative_effects_only", false);
        config.options().copyDefaults(true);
        saveConfig();
        MilkBottle.createMilkBottles();

        MilkPotion milkPotion = new MilkPotion();
        if(config.getBoolean("crafting_enabled")) {
            Bukkit.addRecipe(milkPotion.createRecipe());
        }

        this.getServer().getPluginManager().registerEvents(new MilkBottle(), this);
        this.getServer().getPluginManager().registerEvents(new MilkPotion(), this);

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

    public List<String> splashLore() {
        List<String> stringList = new ArrayList<>();
        stringList.add("&fCleanses all potion effects in an area");

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
