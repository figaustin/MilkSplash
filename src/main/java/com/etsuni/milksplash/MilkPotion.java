package com.etsuni.milksplash;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MilkPotion implements Listener {

    private Plugin plugin = MilkSplash.getPlugin(MilkSplash.class);

    private FileConfiguration config = plugin.getConfig();

    private final Map<UUID, Long> playersOnCooldown = new HashMap<>();
    private Integer cooldownTime;
    private Boolean coolDownsEnabled;
    private Boolean permissionsEnabled;
    private Boolean negativeEffectsOnly;
    private Boolean onlyThrower;
    private ShapedRecipe recipe;

    private Component name;

    private final LegacyComponentSerializer LEGACY_COMP_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public MilkPotion() {
        settings();
    }

    public ShapedRecipe createRecipe() {
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.displayName(name);
        meta.setColor(Color.WHITE);

        List<Component> lore = new ArrayList<>();
        for(String str : config.getStringList("lore")) {
            Component loreComponent = fromLegacy(str);
            lore.add(loreComponent);
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
        name = fromLegacy(config.getString("splash_potion_name"));
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

        Collection<PotionEffect> potionEffects = player.getActivePotionEffects();
        for(PotionEffect potionEffect : potionEffects) {
            PotionEffectType potionType = potionEffect.getType();
            if(negativeEffectsOnly) {
                if(negativeEffects().contains(potionType)) {
                    player.removePotionEffect(potionType);
                }
            }
            else {
                player.removePotionEffect(potionType);
            }
        }
    }

    @EventHandler
    public void onSplash(PotionSplashEvent event){
        ThrownPotion thrownPotion = event.getPotion();
        ItemStack potion = thrownPotion.getItem();

        if(!Objects.equals(potion.getItemMeta().displayName(), fromLegacy(config.getString("splash_potion_name")))){
            return;
        }

        Collection<LivingEntity> entities = event.getAffectedEntities();
        Projectile projectile = event.getEntity();
        LivingEntity player = (LivingEntity) projectile.getShooter();

        if(onlyThrower && entities.contains(player)) {
            Collection<PotionEffect> potionEffects = player.getActivePotionEffects();
            if(negativeEffectsOnly) {
                for(PotionEffect potionEffect : potionEffects) {
                    PotionEffectType type = potionEffect.getType();
                    if(negativeEffects().contains(type)) {
                        player.removePotionEffect(type);
                    }
                }
            }
            else if(potionEffects.size() > 0) {
                for(PotionEffect effect : potionEffects) {
                    PotionEffectType type = effect.getType();
                    player.removePotionEffect(type);
                }
            }
            return;
        }


        for(LivingEntity entity : entities) {
            Collection<PotionEffect> potionEffects = entity.getActivePotionEffects();
            if(negativeEffectsOnly) {
                for(PotionEffect potionEffect : potionEffects) {
                    PotionEffectType type = potionEffect.getType();
                    if(negativeEffects().contains(type)) {
                        entity.removePotionEffect(type);
                    }
                }
            }
            else if(potionEffects.size() > 0) {
                for(PotionEffect effect : potionEffects) {
                    PotionEffectType type = effect.getType();
                    entity.removePotionEffect(type);
                }
            }
        }
    }


    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if(!permissionsEnabled) return;

        Player player = (Player) event.getWhoClicked();

        if(player.hasPermission("milksplash.craft")) return;

        ItemStack item = event.getRecipe().getResult();

        if(item.getItemMeta().displayName().equals(name)) {
            event.setCancelled(true);
            Component msg = fromLegacy(config.getString("no_permission_craft_msg"));
            player.sendMessage(msg);
            player.closeInventory();

        }
    }

    @EventHandler
    public void onThrow(ProjectileLaunchEvent event) {

        Projectile projectile = event.getEntity();
        Player player = (Player) projectile.getShooter();
        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if(!itemStack.isSimilar(MilkBottle.milkBottle)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if(permissionsEnabled) {
            if(!player.hasPermission("milksplash.use")) {
                Component msg = fromLegacy(config.getString("no_permission_throw_msg"));
                player.sendMessage(msg);
                event.setCancelled(true);
                return;
            }
        }

        //Check if cooldowns are enabled and if player is on cool down for potion throwing
        if(coolDownsEnabled) {
            if(playersOnCooldown.containsKey(uuid)) {
                if(playersOnCooldown.get(uuid) > System.currentTimeMillis()){
                    long timeLeft = ((playersOnCooldown.get(uuid) - System.currentTimeMillis()) / 1000);
                    String cdMsg = Objects.requireNonNull(config.getString("cooldown_message")).replace("%seconds%", Long.toString(timeLeft));
                    Component msg = fromLegacy(cdMsg);
                    player.sendMessage(msg);
                    event.setCancelled(true);
                    return;
                }

            }
        }

        playersOnCooldown.put(uuid, System.currentTimeMillis() + (cooldownTime * 1000));

    }

    public Component fromLegacy(String legacyText) {
        return LEGACY_COMP_SERIALIZER.deserialize(legacyText);
    }

    public List<PotionEffectType> negativeEffects() {
        List<PotionEffectType> negEffects = new ArrayList<>();

        negEffects.add(PotionEffectType.SLOW_DIGGING);
        negEffects.add(PotionEffectType.CONFUSION);
        negEffects.add(PotionEffectType.DARKNESS);
        negEffects.add(PotionEffectType.HUNGER);
        negEffects.add(PotionEffectType.POISON);
        negEffects.add(PotionEffectType.SLOW);
        negEffects.add(PotionEffectType.WITHER);
        negEffects.add(PotionEffectType.WEAKNESS);
        negEffects.add(PotionEffectType.UNLUCK);
        negEffects.add(PotionEffectType.LEVITATION);

        return negEffects;
    }
}
