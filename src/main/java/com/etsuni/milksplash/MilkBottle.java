package com.etsuni.milksplash;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitScheduler;

public class MilkBottle implements Listener {

    @EventHandler
    public void brewInvClick(InventoryClickEvent event) {
        Inventory inv = event.getClickedInventory();
        ClickType clickType = event.getClick();
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

        ItemStack clickedItem = event.getCurrentItem(); // GETS ITEMSTACK THAT IS BEING CLICKED
        ItemStack cursorItem = event.getCursor(); // GETS CURRENT ITEMSTACK HELD ON MOUSE
        Player p = (Player) (event.getView().getPlayer());

        if (!cursorItem.getType().equals(Material.MILK_BUCKET)) {
            return;
        }

        p.setItemOnCursor(clickedItem);

        inv.setItem(clickedSlot, cursorItem);
        cursorItem.setAmount(0);
        event.setCancelled(true);


        ItemStack[] itemStacks = inv.getContents();
        Bukkit.broadcast(Component.text(itemStacks[3].toString()));
        if (itemStacks[0] == null && itemStacks[1] == null && itemStacks[2] == null) {
            return;
        }

        startBrewing((BrewerInventory) inv);

    }


    @EventHandler
    public void turnToSplash(InventoryClickEvent event) {

    }

    public void startBrewing(BrewerInventory inventory) {
        BrewClock clock = new BrewClock(inventory, 400);
    }

    public class BrewClock {

        private BrewerInventory inventory;
        private int current;
        private BrewingStand brewingStand;
        private Integer sc;
        private ItemStack[] before;

        public BrewClock(BrewerInventory inventory, int time) {
            this.inventory = inventory;
            this.current = time;
            this.brewingStand = inventory.getHolder();
            this.before = inventory.getContents();
            start();
        }

        public void start() {
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            Bukkit.broadcast(Component.text(brewingStand.getFuelLevel()));

            this.sc = scheduler.scheduleSyncRepeatingTask(MilkSplash.getPlugin(MilkSplash.class), new Runnable() {
                @Override
                public void run() {
                    if (brewingStand.getFuelLevel() < 1) {
                        scheduler.cancelTask(sc);
                        return;
                    }
                    if (current == 0) {

                        inventory.setIngredient(new ItemStack(Material.AIR));
                        ItemStack bucket = new ItemStack(Material.BUCKET, 1);
                        Location location = brewingStand.getLocation();
                        World world = brewingStand.getWorld();
                        world.dropItem(location, bucket);

                        brewingStand.setFuelLevel(brewingStand.getFuelLevel() - 1);
                        ItemStack[] items = inventory.getContents();
                        giveMilkPotion(items);
                        scheduler.cancelTask(sc);
                        return;
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

        public void giveMilkPotion(ItemStack[] items) {
            for (int i = 0; i < items.length - 2; i++) {
                if (items[i] != null && items[i].getType() == Material.POTION) {
                    PotionMeta meta = (PotionMeta) items[i].getItemMeta();
                    meta.setColor(Color.WHITE);
                    meta.displayName(Component.text("Milk Potion"));
                    items[i].setItemMeta(meta);
                }
            }
        }
    }
}
