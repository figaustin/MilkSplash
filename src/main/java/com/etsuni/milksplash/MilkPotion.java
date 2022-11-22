package com.etsuni.milksplash;

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
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;


import java.util.*;

public class MilkPotion implements Listener {

    private Plugin plugin = MilkSplash.plugin;

    private FileConfiguration config = plugin.getConfig();

    private final Map<UUID, Long> playersOnCooldown = new HashMap<>();
    private Integer cooldownTime;
    private Boolean coolDownsEnabled;
    private Boolean permissionsEnabled;
    private Boolean negativeEffectsOnly;
    private Boolean onlyThrower;
    private ShapedRecipe recipe;



    public MilkPotion() {
        settings();
    }

    public ShapedRecipe createRecipe() {

        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setDisplayName(translate(config.getString("splash_potion_name")));
        meta.setColor(Color.WHITE);
        List<String> lore = new ArrayList<>();
        for(String str : config.getStringList("lore")) {
            String loreStr = translate(str);
            lore.add(loreStr);
        }

        meta.setLore(lore);
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
        negativeEffectsOnly = config.getBoolean("negative_effects_only");
        onlyThrower = config.getBoolean("only_cleanse_throwers_effects");
    }


    @EventHandler
    public void onDrink(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if(!item.isSimilar(MilkBottle.milkBottle)) {
            return;
        }

        if (negativeEffectsOnly) {
            removeNegativePotionEffects(player);
        } else {
            removeAllPotionEffects(player);
        }
    }

    @EventHandler
    public void onSplash(PotionSplashEvent event){
        if(!(event.getEntity().getShooter() instanceof LivingEntity)){
            return;
        }

        ThrownPotion thrownPotion = event.getPotion();
        ItemStack potion = thrownPotion.getItem();

        if(!potion.getItemMeta().getDisplayName().equals(translate(config.getString("splash_potion_name")))) {
            return;
        }

        Collection<LivingEntity> affectedEntities = event.getAffectedEntities();
        Projectile projectile = event.getEntity();
        LivingEntity player = (LivingEntity) projectile.getShooter();

        if(onlyThrower && affectedEntities.contains(player)) {
            if(negativeEffectsOnly) {
                removeNegativePotionEffects(player);
            }
            else {
                removeAllPotionEffects(player);
            }

            return;
        }

        if (negativeEffectsOnly) {
            affectedEntities.forEach(this::removeNegativePotionEffects);
        } else {
            affectedEntities.forEach(this::removeAllPotionEffects);
        }
    }


    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if(!permissionsEnabled) return;

        Player player = (Player) event.getWhoClicked();

        if(player.hasPermission("milksplash.craft")) return;

        ItemStack item = event.getRecipe().getResult();

        if(item.getItemMeta().getDisplayName().equals(translate(config.getString("splash_potion_name")))) {
            event.setCancelled(true);
            player.sendMessage(translate(config.getString("no_permission_craft_msg")));
            player.closeInventory();
        }
    }

    @EventHandler
    public void onThrow(ProjectileLaunchEvent event) {
        if(!(event.getEntity().getShooter() instanceof LivingEntity)){
            return;
        }
        if(!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity().getShooter();
        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if(!itemStack.isSimilar(MilkBottle.milkBottle)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if(permissionsEnabled) {
            if(!player.hasPermission("milksplash.use")) {
                player.sendMessage(translate(config.getString("no_permission_throw_msg")));
                event.setCancelled(true);
                return;
            }
        }

        if(coolDownsEnabled) {
            if(playersOnCooldown.containsKey(uuid)) {
                if(playersOnCooldown.get(uuid) > System.currentTimeMillis()){
                    long timeLeft = ((playersOnCooldown.get(uuid) - System.currentTimeMillis()) / 1000);
                    String cdMsg = Objects.requireNonNull(config.getString("cooldown_message")).replace("%seconds%", Long.toString(timeLeft));
                    player.sendMessage(translate(cdMsg));
                    event.setCancelled(true);
                    return;
                }

            }
        }

        playersOnCooldown.put(uuid, System.currentTimeMillis() + (cooldownTime * 1000));

    }

    public String translate(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public List<PotionEffectType> negativeEffects() {
        List<PotionEffectType> negEffects = new ArrayList<>();

        negEffects.add(PotionEffectType.SLOW_DIGGING);
        negEffects.add(PotionEffectType.CONFUSION);

        if(MilkSplash.VERSION.equals("1.19.2")) {
            negEffects.add(PotionEffectType.DARKNESS);
        }
        negEffects.add(PotionEffectType.HUNGER);
        negEffects.add(PotionEffectType.POISON);
        negEffects.add(PotionEffectType.SLOW);
        negEffects.add(PotionEffectType.WITHER);
        negEffects.add(PotionEffectType.WEAKNESS);
        negEffects.add(PotionEffectType.UNLUCK);
        negEffects.add(PotionEffectType.LEVITATION);

        return negEffects;
    }

    private boolean isNegativePotionEffect(PotionEffectType effect) {
        return negativeEffects().contains(effect);
    }

    // Overloaded above just because
    private boolean isNegativePotionEffect(PotionEffect effect) {
        return isNegativePotionEffect(effect.getType());
    }

    private void removeNegativePotionEffects(LivingEntity entity) {
        entity.getActivePotionEffects().stream()
                .map(PotionEffect::getType)
                .filter(this::isNegativePotionEffect)
                .forEach(entity::removePotionEffect);
    }

    private void removeAllPotionEffects(LivingEntity entity) {
        entity.getActivePotionEffects().stream()
                .map(PotionEffect::getType)
                .forEach(entity::removePotionEffect);
    }
}
