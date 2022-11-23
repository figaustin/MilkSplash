package com.etsuni.milksplash;

import org.bukkit.block.BrewingStand;

import java.util.ArrayList;
import java.util.List;

public class BrewingList {

    private List<BrewingStand> brewingStands = new ArrayList<>();

    private static BrewingList instance = new BrewingList();

    private BrewingList() {

    }

    public static BrewingList getInstance() {
        return instance;
    }

    public void add(BrewingStand stand) {
        brewingStands.add(stand);
    }

    public void remove(BrewingStand stand) {
        brewingStands.remove(stand);
    }

    public List<BrewingStand> getList() {
        return brewingStands;
    }

}
