package dev.wndrxz.gravedig.grave;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;

/**
 * splits a death inventory snapshot into brush portions.
 * pure on purpose -- junit covers it without a server.
 */
public final class PortionSplitter {

    private PortionSplitter() {}

    /**
     * armor = armor slots + offhand (it's "equipment" for our purposes),
     * hotbar = storage slots 0..8, rest = 9..35.
     * empty portions are skipped so digging never gives a dead click.
     */
    public static List<Portion> split(ItemStack[] armor, ItemStack offhand, ItemStack[] storage) {
        List<ItemStack> armorItems = new ArrayList<>();
        addAll(armorItems, armor);
        add(armorItems, offhand);

        List<ItemStack> hotbar = new ArrayList<>();
        List<ItemStack> rest = new ArrayList<>();
        if (storage != null) {
            for (int slot = 0; slot < storage.length; slot++) {
                add(slot <= 8 ? hotbar : rest, storage[slot]);
            }
        }

        List<Portion> portions = new ArrayList<>(3);
        if (!armorItems.isEmpty()) portions.add(new Portion(Portion.Type.ARMOR, armorItems));
        if (!hotbar.isEmpty()) portions.add(new Portion(Portion.Type.HOTBAR, hotbar));
        if (!rest.isEmpty()) portions.add(new Portion(Portion.Type.REST, rest));
        return portions;
    }

    private static void addAll(List<ItemStack> out, ItemStack[] src) {
        if (src == null) return;
        for (ItemStack item : src) {
            add(out, item);
        }
    }

    private static void add(List<ItemStack> out, ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) return;
        out.add(item.clone());
    }
}
