package com.etsuni.milksplash;

import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;

import java.util.ArrayList;
import java.util.List;

public class BrewingList {

    private static List<BrewingStand> brewingStands = new ArrayList<>();

    private BrewingList() {

    }

    public static void add(BrewingStand stand) {
        brewingStands.add(stand);
    }

    public static void remove(BrewingStand stand) {
        brewingStands.remove(stand);
    }

    public static List<BrewingStand> getList() {
        return brewingStands;
    }

}
