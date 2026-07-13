package dev.wndrxz.gravedig.grave;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// plain ItemStacks without meta work fine off-server, so no mocks needed
class PortionSplitterTest {

    private ItemStack[] storage(ItemStack hotbarItem, ItemStack restItem) {
        ItemStack[] s = new ItemStack[36];
        s[0] = hotbarItem;
        s[20] = restItem;
        return s;
    }

    @Test
    void splitsIntoThreePortionsInDigOrder() {
        ItemStack[] armor = {new ItemStack(Material.IRON_BOOTS), null, null, null};
        List<Portion> portions = PortionSplitter.split(armor,
                new ItemStack(Material.SHIELD),
                storage(new ItemStack(Material.DIAMOND_SWORD), new ItemStack(Material.DIRT, 32)));
        assertEquals(3, portions.size());
        assertEquals(Portion.Type.ARMOR, portions.get(0).type());
        assertEquals(Portion.Type.HOTBAR, portions.get(1).type());
        assertEquals(Portion.Type.REST, portions.get(2).type());
        assertEquals(2, portions.get(0).items().size()); // boots + offhand shield
    }

    @Test
    void skipsEmptyPortions() {
        List<Portion> portions = PortionSplitter.split(null, null,
                storage(new ItemStack(Material.TORCH), null));
        assertEquals(1, portions.size());
        assertEquals(Portion.Type.HOTBAR, portions.get(0).type());
    }

    @Test
    void ignoresAirAndZeroAmounts() {
        ItemStack[] armor = {new ItemStack(Material.AIR), null};
        List<Portion> portions = PortionSplitter.split(armor, null, null);
        assertTrue(portions.isEmpty());
    }

    @Test
    void hotbarIsSlotsZeroToEight() {
        ItemStack[] s = new ItemStack[36];
        s[8] = new ItemStack(Material.TORCH);
        s[9] = new ItemStack(Material.COBBLESTONE);
        List<Portion> portions = PortionSplitter.split(null, null, s);
        assertEquals(2, portions.size());
        assertEquals(Portion.Type.HOTBAR, portions.get(0).type());
        assertEquals(Material.TORCH, portions.get(0).items().get(0).getType());
        assertEquals(Portion.Type.REST, portions.get(1).type());
        assertEquals(Material.COBBLESTONE, portions.get(1).items().get(0).getType());
    }
}
