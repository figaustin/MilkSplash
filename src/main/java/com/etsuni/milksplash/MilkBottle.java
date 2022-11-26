package com.etsuni.milksplash;

import org.bukkit.*;
import org.bukkit.block.BrewingStand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;


public class MilkBottle implements Listener {
    private static Plugin plugin = MilkSplash.plugin;

    private static FileConfiguration config = plugin.getConfig();

    public static ItemStack milkBottle;
    public static ItemStack splashMilk;


    public static void createMilkBottles() {
        milkBottle = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) milkBottle.getItemMeta();
        String name = translate(config.getString("regular_potion_name"));

        List<String> lore1 = new ArrayList<>();
        for(String str : config.getStringList("regular_potion_lore")) {
            String loreStr = translate(str);
            lore1.add(loreStr);
        }
        meta.setColor(Color.WHITE);
        meta.setLore(lore1);

        meta.setDisplayName(name);
        milkBottle.setItemMeta(meta);

        List<String> lore2 = new ArrayList<>();
        for(String str : config.getStringList("splash_potion_lore")) {
            String loreStr = translate(str);
            lore2.add(loreStr);
        }
        splashMilk = new ItemStack(Material.SPLASH_POTION);
        PotionMeta splashMeta = (PotionMeta) splashMilk.getItemMeta();

        splashMeta.setColor(Color.WHITE);
        splashMeta.setDisplayName(translate(config.getString("splash_potion_name")));
        splashMeta.setLore(lore2);
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

        int clickedSlot = event.getSlot();

        BrewingStand stand;

        if(inv.getType().equals(InventoryType.BREWING) && clickedSlot == 4 ) {
            stand = ((BrewerInventory) inv).getHolder();
            if(BrewingList.getInstance().getList().contains(stand)) {
                event.setCancelled(true);
            }
        }

        if (!clickType.isLeftClick()) {
            return;
        }
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
            player.sendMessage(translate(config.getString("no_permission_brew_msg")));
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
        if(!inv.contains(milkBottle)) {
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

    public static String translate(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

}
