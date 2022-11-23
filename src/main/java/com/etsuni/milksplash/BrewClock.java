package com.etsuni.milksplash;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class BrewClock{

    private BrewerInventory inventory;
    private int current;
    private BrewingStand brewingStand;
    private Integer sc;
    private ItemStack[] before;
    private Boolean splash;

    private Plugin plugin = MilkSplash.plugin;

    private FileConfiguration config = plugin.getConfig();

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
        BrewingList.getInstance().getList().add(brewingStand);
        this.sc = scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (brewingStand.getFuelLevel() < 1) {
                    BrewingList.getInstance().getList().remove(brewingStand);
                    scheduler.cancelTask(sc);
                    return;
                }
                if (current == 0) {
                    ItemStack[] items = inventory.getContents();
                    inventory.setIngredient(new ItemStack(Material.AIR));
                    MilkBottle milkBottle = new MilkBottle();
                    if (splash) {
                        milkBottle.giveMilkPotion(items, true);
                    } else {
                        ItemStack bucket = new ItemStack(Material.BUCKET, 1);
                        inventory.setIngredient(bucket);
                        milkBottle.giveMilkPotion(items, false);
                        BrewingList.getInstance().getList().remove(brewingStand);
                        scheduler.cancelTask(sc);
                        return;
                    }
                }
                if (!searchChanged(before, inventory.getContents())) {
                    BrewingList.getInstance().getList().remove(brewingStand);
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
        for (int i = 0; i < before.length - 1; i++) {
            if (before[i] == null) {
                continue;
            }
            if ((before[i] != null && after[i] == null) || (before[i] == null && after[i] != null)) {
                return false;
            } else {
                if (!(before[i].getType() == after[i].getType())) {
                    return false;
                }
            }
        }
        return true;
    }
}
