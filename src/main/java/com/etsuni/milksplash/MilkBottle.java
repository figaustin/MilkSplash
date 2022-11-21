package com.etsuni.milksplash;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.BrewingStand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MilkBottle implements Listener {
    private static Plugin plugin = MilkSplash.getPlugin(MilkSplash.class);

    private static FileConfiguration config = plugin.getConfig();

    private final static LegacyComponentSerializer LEGACY_COMP_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public static ItemStack milkBottle;
    public static ItemStack splashMilk;

    public static void createMilkBottles() {
        milkBottle = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) milkBottle.getItemMeta();
        Component name = fromLegacy(config.getString("regular_potion_name"));
        List<Component> lore1 = new ArrayList<>();
        for(String str : config.getStringList("regular_potion_lore")) {
            Component loreComponent = fromLegacy(str);
            lore1.add(loreComponent);
        }
        meta.setColor(Color.WHITE);
        meta.lore(lore1);
        meta.displayName(name);
        milkBottle.setItemMeta(meta);

        List<Component> lore2 = new ArrayList<>();
        for(String str : config.getStringList("splash_potion_lore")) {
            Component loreComponent = fromLegacy(str);
            lore2.add(loreComponent);
        }
        splashMilk = new ItemStack(Material.SPLASH_POTION);
        PotionMeta splashMeta = (PotionMeta) splashMilk.getItemMeta();
        Component splashName = fromLegacy(config.getString("splash_potion_name"));
        splashMeta.setColor(Color.WHITE);
        splashMeta.displayName(splashName);
        splashMeta.lore(lore2);
        splashMilk.setItemMeta(splashMeta);
    }

    @EventHandler
    public void brewInvClick(InventoryClickEvent event) {
        Inventory inv = event.getClickedInventory();
        ClickType clickType = event.getClick();
        Player player = (Player) event.getWhoClicked();
        if (inv == null || !inv.getType().equals(InventoryType.BREWING)) {
            return;
        }



        if (!clickType.isLeftClick()) {
            return;
        }
        int clickedSlot = event.getSlot();
        if (clickedSlot != 3) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        Player p = (Player) (event.getView().getPlayer());

        if (!cursorItem.getType().equals(Material.MILK_BUCKET)) {
            return;
        }

        if(!config.getBoolean("brewing_enabled")) {
            return;
        }

        if(config.getBoolean("permissions_enabled") && !player.hasPermission("milksplash.brew")) {
            player.sendMessage(fromLegacy(config.getString("no_permission_brew_msg")));
            return;
        }

        p.setItemOnCursor(clickedItem);

        inv.setItem(clickedSlot, cursorItem);
        cursorItem.setAmount(0);
        event.setCancelled(true);


        ItemStack[] itemStacks = inv.getContents();
        if (itemStacks[0] == null && itemStacks[1] == null && itemStacks[2] == null) {
            return;
        }



        Boolean foundMilk = false;

        Integer milkCounter = 0;
        Integer awkwardCounter = 0;
        for(int i = 0; i < 3; i++) {
            if(itemStacks[i] == null) {
                continue;
            }
            milkCounter = isMilkBottle(itemStacks[i]) ? milkCounter + 1 : milkCounter - 1;
            awkwardCounter = isAwkwardPotion(itemStacks[i]) ? awkwardCounter + 1 : awkwardCounter - 1;
        }

        if(awkwardCounter > 0) {
            startBrewing((BrewerInventory) inv, foundMilk);
        }

        if(milkCounter > 0){
            foundMilk = true;
            startBrewing((BrewerInventory) inv, foundMilk);
        }
    }

    @EventHandler
    public void onGunpowderBrew(BrewEvent event) {
        ItemStack ingredient = event.getContents().getIngredient();
        BrewerInventory inv = event.getContents();

        if(!ingredient.getType().equals(Material.GUNPOWDER)) {
            return;
        }
        ItemStack[] items = inv.getContents();
        giveMilkPotion(items, true);
        inv.setIngredient(new ItemStack(Material.AIR));
        event.setCancelled(true);
    }


    public void startBrewing(BrewerInventory inventory, Boolean splash) {
        BrewClock clock = new BrewClock(inventory, 400);
    }

    public class BrewClock {

        private BrewerInventory inventory;
        private int current;
        private BrewingStand brewingStand;
        private Integer sc;
        private ItemStack[] before;
        private Boolean splash;

        public BrewClock(BrewerInventory inventory, int time) {
            this.inventory = inventory;
            this.current = time;
            this.brewingStand = inventory.getHolder();
            this.before = inventory.getContents();
            this.splash = false;
            start();
        }

        public void start() {
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            brewingStand.setFuelLevel(brewingStand.getFuelLevel() - 1);

            this.sc = scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (brewingStand.getFuelLevel() < 1) {
                        scheduler.cancelTask(sc);
                        return;
                    }
                    if (current == 0) {
                        ItemStack[] items = inventory.getContents();
                        inventory.setIngredient(new ItemStack(Material.AIR));
                        if(splash) {
                            giveMilkPotion(items, true);
                        }
                        else{
                            ItemStack bucket = new ItemStack(Material.BUCKET, 1);
                            inventory.setIngredient(bucket);
                            giveMilkPotion(items, false);
                            scheduler.cancelTask(sc);
                            return;
                        }
                    }
                    if(!searchChanged(before, inventory.getContents())) {
                        scheduler.cancelTask(sc);
                        return;
                    }
                    current--;
                    brewingStand.setBrewingTime(current);
                    brewingStand.update(true);
                }
            }, 0, 0);
        }

        public boolean searchChanged(ItemStack[] before, ItemStack[] after) {
            for(int i = 0; i < before.length; i++) {
                if(before[i] == null) {
                    continue;
                }

                if((before[i] != null && after[i] == null) || (before[i] == null && after[i] != null)) {
                    return false;
                }
                 else{
                     if(!(before[i].getType() == after[i].getType())){
                         return false;
                     }
                }
            }
            return true;
        }
    }

    public void giveMilkPotion(ItemStack[] items, Boolean splash) {
        for (int i = 0; i < items.length - 2; i++) {
            if(items[i] == null) {
                continue;
            }
            if(splash) {
                if (items[i] != null && isMilkBottle(items[i])) {
                    items[i].setType(splashMilk.getType());
                    items[i].setItemMeta(splashMilk.getItemMeta());
                }
            }
            else if (items[i] != null && isAwkwardPotion(items[i])) {
                items[i].setType(milkBottle.getType());
                items[i].setItemMeta(milkBottle.getItemMeta());
            }
        }
    }

    public static Component fromLegacy(String legacyText) {
        return LEGACY_COMP_SERIALIZER.deserialize(legacyText);
    }

    public Boolean isAwkwardPotion(ItemStack potion) {
        if(potion == null) {
            return false;
        }

        if(!potion.getType().equals(Material.POTION)) {
            return false;
        }

        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        PotionType type = meta.getBasePotionData().getType();

        return type == PotionType.AWKWARD;
    }

    public Boolean isMilkBottle(ItemStack item) {
        if(item == null) {
            return false;
        }

        return item.isSimilar(milkBottle);
    }

}
