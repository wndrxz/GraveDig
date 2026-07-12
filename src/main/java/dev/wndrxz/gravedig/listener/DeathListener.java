package dev.wndrxz.gravedig.listener;

import java.util.ArrayList;
import java.util.List;
import dev.wndrxz.gravedig.config.ConfigManager;
import dev.wndrxz.gravedig.grave.GraveManager;
import dev.wndrxz.gravedig.grave.Portion;
import dev.wndrxz.gravedig.grave.PortionSplitter;
import dev.wndrxz.gravedig.grave.XpMath;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class DeathListener implements Listener {

    private final ConfigManager cfg;
    private final GraveManager graves;

    public DeathListener(ConfigManager cfg, GraveManager graves) {
        this.cfg = cfg;
        this.graves = graves;
    }

    // HIGH so plugins tweaking drops on NORMAL run before we take over
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (!cfg.enabled()) return;
        if (event.getKeepInventory()) return;
        Player player = event.getEntity();

        PlayerInventory inv = player.getInventory();
        ItemStack offhand = vanishing(inv.getItemInOffHand()) ? null : inv.getItemInOffHand();
        List<Portion> portions = PortionSplitter.split(
                withoutVanishing(inv.getArmorContents()), offhand,
                withoutVanishing(inv.getStorageContents()));

        // paper keeps the inventory readable during the event. if some fork
        // cleared it early, fall back to the plain drops list as one portion
        if (portions.isEmpty() && !event.getDrops().isEmpty()) {
            List<ItemStack> copy = new ArrayList<>();
            for (ItemStack it : event.getDrops()) copy.add(it.clone());
            portions = List.of(new Portion(Portion.Type.REST, copy));
        }

        int xp = 0;
        if (cfg.xpEnabled() && !event.getKeepLevel()) {
            xp = (int) Math.floor(
                    XpMath.totalPoints(player.getLevel(), player.getExp()) * cfg.xpPercent());
        }
        if (portions.isEmpty() && xp <= 0) return;

        // TODO items other plugins injected into drops get eaten here when we
        // captured slots ourselves. matching them up is not worth it yet
        event.getDrops().clear();
        event.setDroppedExp(0);

        graves.create(player, portions, xp);
    }

    private boolean vanishing(ItemStack it) {
        return it != null && it.containsEnchantment(Enchantment.VANISHING_CURSE);
    }

    private ItemStack[] withoutVanishing(ItemStack[] src) {
        if (src == null) return null;
        ItemStack[] out = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            if (!vanishing(src[i])) out[i] = src[i];
        }
        return out;
    }
}
