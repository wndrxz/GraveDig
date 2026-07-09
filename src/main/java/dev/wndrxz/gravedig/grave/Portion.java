package dev.wndrxz.gravedig.grave;

import java.util.List;
import org.bukkit.inventory.ItemStack;

/** one brushable chunk of the drop. order matters: armor first, junk last */
public final class Portion {

    public enum Type { ARMOR, HOTBAR, REST }

    private final Type type;
    private final List<ItemStack> items;

    public Portion(Type type, List<ItemStack> items) {
        this.type = type;
        this.items = items;
    }

    public Type type() { return type; }
    public List<ItemStack> items() { return items; }
}
