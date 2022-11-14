package com.etsuni.milksplash;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.checkerframework.checker.units.qual.N;

import java.util.*;

public class MilkPotion implements Listener {

    private Plugin plugin = MilkSplash.getPlugin(MilkSplash.class);

    private FileConfiguration config = plugin.getConfig();

    private final Map<UUID, Long> playersOnCooldown = new HashMap<>();
    private Integer cooldownTime;
    private Boolean coolDownsEnabled;
    private Boolean permissionsEnabled;
    private ShapedRecipe recipe;

    public MilkPotion() {
        settings();
    }

    public ShapedRecipe createRecipe() {
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();

        meta.displayName(Component.text(Objects.requireNonNull(config.getString("item_name"))));
        meta.setColor(Color.WHITE);

        List<Component> lore = new ArrayList<>();
        for(String string : config.getStringList("lore")) {
            lore.add(Component.text(string));
        }

        meta.lore(lore);
        item.setItemMeta(meta);

        NamespacedKey key = new NamespacedKey(plugin, "milk_bottle");
        recipe = new ShapedRecipe(key, item);

        recipe.shape("M", "G", "W");

        recipe.setIngredient('M', Material.MILK_BUCKET);
        recipe.setIngredient('W', Material.GLASS_BOTTLE);
        recipe.setIngredient('G', Material.GUNPOWDER);

        return recipe;
    }

    public void settings() {
        cooldownTime = config.getInt("cooldown_time");
        coolDownsEnabled = config.getBoolean("cooldowns_enabled");
        permissionsEnabled = config.getBoolean("permissions_enabled");
    }


    //TODO MAKE IT SO IT WILL ONLY REMOVE NEGATIVE EFFECTS ?
    @EventHandler
    public void onSplash(PotionSplashEvent event){
        ThrownPotion thrownPotion = event.getPotion();
        ItemStack potion = thrownPotion.getItem();
        Collection<LivingEntity> entities = event.getAffectedEntities();


        if(!potion.getItemMeta().displayName().toString().contains("Milk Bottle")){
            Bukkit.broadcast(Component.text("Didn't work"));
            return;
        }

        for(LivingEntity entity : entities) {
            Collection<PotionEffect> potionEffects = entity.getActivePotionEffects();
            if(potionEffects.size() > 0) {
                for(PotionEffect effect : potionEffects) {
                    PotionEffectType type = effect.getType();
                    entity.removePotionEffect(type);
                }
                Bukkit.broadcast(Component.text("Potions cleared"));
            }

        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        Player player = (Player) event.getWhoClicked();

        if(player.hasPermission("milksplash.craft")) return;

        ItemStack item = event.getRecipe().getResult();

        String name = ChatColor.translateAlternateColorCodes('&', config.getString("item_name"));

        if(item.getItemMeta().displayName().toString().contains(name)) {
            event.setCancelled(true);
            String msg = ChatColor.translateAlternateColorCodes('&', config.getString("no_permissions_craft_msg"));
            player.sendMessage(Component.text(msg));
            player.closeInventory();

        }

    }

    @EventHandler
    public void onThrow(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        Player player = (Player) projectile.getShooter();
        UUID uuid = player.getUniqueId();

        if(!player.hasPermission("milksplash.use")) {
            String msg = ChatColor.translateAlternateColorCodes('&', config.getString("no_permissions_throw_msg"));
            player.sendMessage(Component.text(msg));
            event.setCancelled(true);
            return;
        }

        //Check if player is on cool down for potion throwing
        if(playersOnCooldown.containsKey(uuid)) {
            if(playersOnCooldown.get(uuid) > System.currentTimeMillis()){
                long timeLeft = (playersOnCooldown.get(uuid) - System.currentTimeMillis() / 1000);
                player.sendMessage(Component.text("You can't do that for " + timeLeft + " more seconds!"));
                event.setCancelled(true);
                return;
            }

        }

        playersOnCooldown.put(uuid, System.currentTimeMillis() + (cooldownTime * 1000));


    }
}
