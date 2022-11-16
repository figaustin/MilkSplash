package com.etsuni.milksplash;


import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class MilkBottle implements Listener {

    private BrewClock clock;

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

        ItemStack clickedItem = event.getCurrentItem(); // GETS ITEMSTACK THAT IS BEING CLICKED
        ItemStack cursorItem = event.getCursor(); // GETS CURRENT ITEMSTACK HELD ON MOUSE

        if(!cursorItem.getType().equals(Material.MILK_BUCKET)) {
            return;
        }

        if (event.getClick() == ClickType.RIGHT && clickedItem.isSimilar(cursorItem)) {
            return;
        }
        Integer clickedSlot = event.getSlot();
        if(clickedSlot != 3) {
            return;
        }

        inv.setItem(clickedSlot, cursorItem);
        cursorItem.setAmount(0);
        event.setCancelled(true);

        Player p = (Player) (event.getView().getPlayer());

        if(inv.getContents().length < 1) {
            return;
        }
        startBrewing((BrewerInventory) inv);

    }


    public void startBrewing(BrewerInventory inventory) {
        clock = new BrewClock(inventory, 400);
    }

    public class BrewClock extends BukkitRunnable {

        private BrewerInventory inventory;
        private int current;
        private BrewingStand brewingStand;

        public BrewClock(BrewerInventory inventory, int time) {
            this.inventory = inventory;
            this.current = time;
            this.brewingStand = inventory.getHolder();
            runTaskTimer(MilkSplash.getPlugin(MilkSplash.class), 0L, 0L);
        }

        @Override
        public void run() {
            if(brewingStand.getFuelLevel() < 1) {
                cancel();
                return;
            }
            if(current == 0) {
                if(inventory.getIngredient().getAmount() > 0) {
                    inventory.setIngredient(new ItemStack(Material.AIR));
                    ItemStack bucket = new ItemStack(Material.BUCKET);
                    Location location = brewingStand.getLocation();
                    World world = brewingStand.getWorld();
                    world.dropItem(location, bucket);
                } else {
                    inventory.setIngredient(new ItemStack(Material.AIR));
                }
                brewingStand.setFuelLevel(brewingStand.getFuelLevel() - 1);
                ItemStack[] items = inventory.getContents();
                giveMilkPotion(inventory, items);
                cancel();
                return;

            }
            current--;
            brewingStand.setBrewingTime(current);
            brewingStand.update(true);
        }

    }

    public void giveMilkPotion(BrewerInventory inventory, ItemStack[] items ){
        for(ItemStack item : items) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            meta.setColor(Color.WHITE);
            meta.displayName(Component.text("Milk Potion"));
            item.setItemMeta(meta);
        }
    }
}
