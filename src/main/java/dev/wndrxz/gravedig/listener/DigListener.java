package dev.wndrxz.gravedig.listener;

import dev.wndrxz.gravedig.config.ConfigManager;
import dev.wndrxz.gravedig.grave.Grave;
import dev.wndrxz.gravedig.grave.GraveManager;
import dev.wndrxz.gravedig.locale.LocaleManager;
import dev.wndrxz.gravedig.util.BlockKey;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class DigListener implements Listener {

    private final ConfigManager cfg;
    private final LocaleManager locale;
    private final GraveManager graves;

    public DigListener(ConfigManager cfg, LocaleManager locale, GraveManager graves) {
        this.cfg = cfg;
        this.locale = locale;
        this.graves = graves;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return; // offhand fires a twin event
        Block block = event.getClickedBlock();
        if (block == null) return;
        Grave grave = graves.get(BlockKey.of(block));
        if (grave == null) return;
        // always cancel: vanilla brushing runs loot tables and would eat the block
        event.setCancelled(true);
        if (event.getItem() == null || event.getItem().getType() != Material.BRUSH) return;
        graves.brush(event.getPlayer(), block, grave);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Grave grave = graves.get(BlockKey.of(event.getBlock()));
        if (grave == null) return;
        if (graves.isProtected(event.getPlayer(), grave)) {
            event.setCancelled(true);
            locale.send(event.getPlayer(), "dig.protected",
                    Placeholder.unparsed("owner", grave.ownerName()));
            return;
        }
        if (!cfg.breakDropsAll()) {
            event.setCancelled(true);
            return;
        }
        event.setDropItems(false);
        event.setExpToDrop(0);
        graves.dumpAll(grave, event.getBlock());
        locale.send(event.getPlayer(), "dig.broke-all");
    }

    // suspicious blocks have gravity. a falling grave lands somewhere else (or breaks)
    // while the plugin still points at the old spot, so graves simply don't fall
    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) return;
        if (graves.get(BlockKey.of(event.getBlock())) != null) event.setCancelled(true);
    }

    // graves shouldn't pop from creeper holes
    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> graves.get(BlockKey.of(b)) != null);
    }

    // ...and pistons don't get to play with them either. simpler to veto the
    // whole push than to chase the block around and rekey the grave
    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (graves.get(BlockKey.of(b)) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (graves.get(BlockKey.of(b)) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> graves.get(BlockKey.of(b)) != null);
    }
}
