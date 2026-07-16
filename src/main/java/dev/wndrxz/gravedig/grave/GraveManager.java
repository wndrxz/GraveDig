package dev.wndrxz.gravedig.grave;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.wndrxz.gravedig.config.ConfigManager;
import dev.wndrxz.gravedig.locale.LocaleManager;
import dev.wndrxz.gravedig.persistence.StateStore;
import dev.wndrxz.gravedig.sched.Sched;
import dev.wndrxz.gravedig.util.BlockKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** owns the grave map and everything that happens to grave blocks */
public final class GraveManager {

    private final JavaPlugin plugin;
    private final ConfigManager cfg;
    private final LocaleManager locale;
    private final Sched sched;
    private final StateStore store;

    private final Map<BlockKey, Grave> graves = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();

    public GraveManager(JavaPlugin plugin, ConfigManager cfg, LocaleManager locale,
                        Sched sched, StateStore store) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.locale = locale;
        this.sched = sched;
        this.store = store;
    }

    public Grave get(BlockKey key) {
        return graves.get(key);
    }

    public List<Grave> byOwner(UUID owner) {
        List<Grave> out = new ArrayList<>();
        for (Grave g : graves.values()) if (g.owner().equals(owner)) out.add(g);
        return out;
    }

    // -- creation --

    public void create(Player player, List<Portion> portions, int xp) {
        Block spot = findSpot(player.getLocation());
        Material mat = chooseMaterial(spot);
        spot.setType(mat, false); // no physics! suspicious blocks have gravity and WILL take a dive
        BlockKey key = BlockKey.of(spot);
        Grave grave = new Grave(key, player.getUniqueId(), player.getName(),
                System.currentTimeMillis(), mat, portions, xp);
        graves.put(key, grave);
        persist();

        if (cfg.deathCoords()) {
            locale.send(player, "death.grave-created",
                    Placeholder.component("coords", coords(key)));
        }
        if (cfg.protectMs() > 0) {
            locale.send(player, "death.protected-for",
                    Placeholder.unparsed("minutes", String.valueOf(cfg.protectMs() / 60_000L)));
        }
    }

    private Block findSpot(Location death) {
        World world = death.getWorld();
        int y = Math.max(world.getMinHeight(), Math.min(death.getBlockY(), world.getMaxHeight() - 1));
        Block feet = world.getBlockAt(death.getBlockX(), y, death.getBlockZ());
        if (!canHostGrave(feet)) {
            // died inside a wall? poke upward a bit
            for (int i = 1; i < 5 && feet.getY() + i < world.getMaxHeight(); i++) {
                Block c = feet.getRelative(0, i, 0);
                if (canHostGrave(c)) return c;
            }
            return feet; // give up and overwrite. rude, but items beat a flower
        }
        // died mid-air: sink to the ground, nobody looks for a grave in the sky
        Block spot = feet;
        while (spot.getY() > world.getMinHeight() && sinkable(spot.getRelative(0, -1, 0))) {
            spot = spot.getRelative(0, -1, 0);
        }
        return spot;
    }

    /** air, liquid or squishy stuff (grass, flowers, snow) - fine to swap for a grave */
    private boolean canHostGrave(Block b) {
        return b.getType().isAir() || b.isLiquid() || Tag.REPLACEABLE.isTagged(b.getType());
    }

    /** sink through air and plants, but rest on top of liquids, not at the bottom */
    private boolean sinkable(Block below) {
        if (below.isLiquid()) return false;
        return below.getType().isAir() || Tag.REPLACEABLE.isTagged(below.getType());
    }

    private Material chooseMaterial(Block spot) {
        if (cfg.graveBlock() != null) return cfg.graveBlock();
        Material below = spot.getRelative(0, -1, 0).getType();
        return switch (below) {
            case SAND, RED_SAND, SANDSTONE, RED_SANDSTONE, SMOOTH_SANDSTONE ->
                    Material.SUSPICIOUS_SAND;
            default -> Material.SUSPICIOUS_GRAVEL;
        };
    }

    // -- digging --

    public boolean isProtected(Player digger, Grave grave) {
        return grave.isProtectedFrom(digger.getUniqueId(), System.currentTimeMillis(), cfg.protectMs());
    }

    public void brush(Player digger, Block block, Grave grave) {
        if (isProtected(digger, grave)) {
            locale.send(digger, "dig.protected", Placeholder.unparsed("owner", grave.ownerName()));
            return;
        }
        long now = System.currentTimeMillis();
        Long prev = lastClick.put(digger.getUniqueId(), now);
        if (prev != null && now - prev < 200) return; // holding rmb machine-guns events

        if (cfg.digEffects()) {
            block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5),
                    Sound.ITEM_BRUSH_BRUSHING_GENERIC, 1f, 1f);
            block.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                    block.getLocation().add(0.5, 1.0, 0.5), 8, 0.2, 0.1, 0.2, block.getBlockData());
        }

        Portion done = grave.brushClick(cfg.clicksPerPortion());
        if (done == null && grave.portionsLeft() > 0) return; // mid-portion, nothing to do
        if (done != null) {
            dropItems(block, done.items());
            if (cfg.digEffects()) {
                block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5),
                        Sound.ITEM_BRUSH_BRUSHING_GRAVEL_COMPLETE, 1f, 1f);
            }
            locale.send(digger, "dig.portion-" + done.type().name().toLowerCase(Locale.ROOT));
        }
        if (grave.portionsLeft() > 0) {
            persist();
            return;
        }
        int xp = grave.takeXp();
        if (xp > 0) {
            digger.giveExp(xp);
            locale.send(digger, "dig.xp", Placeholder.unparsed("xp", String.valueOf(xp)));
        }
        finish(block, grave);
        locale.send(digger, "dig.done");
    }

    private void dropItems(Block block, List<ItemStack> items) {
        Location at = block.getLocation().add(0.5, 1.1, 0.5);
        for (ItemStack it : items) block.getWorld().dropItemNaturally(at, it);
    }

    private void finish(Block block, Grave grave) {
        graves.remove(grave.key());
        if (block.getType() == grave.placedAs()) block.setType(Material.AIR);
        persist();
    }

    /** block got broken (or admin said so): everything out at once */
    public void dumpAll(Grave grave, Block block) {
        if (graves.remove(grave.key()) == null) return;
        List<ItemStack> everything = new ArrayList<>();
        for (Portion p : grave.portions()) everything.addAll(p.items());
        dropItems(block, everything);
        int xp = grave.takeXp();
        if (xp > 0) {
            ExperienceOrb orb = block.getWorld().spawn(
                    block.getLocation().add(0.5, 1.0, 0.5), ExperienceOrb.class);
            orb.setExperience(xp);
        }
        persist();
    }

    // -- expiry sweep, runs on the global scheduler --

    private int sweepPasses; // orphan checks tick on a slower beat than expiry

    public void sweep() {
        long now = System.currentTimeMillis();
        boolean orphanPass = ++sweepPasses % 30 == 0;
        for (Grave grave : graves.values()) {
            if (grave.protectionJustEnded(now, cfg.protectMs())) {
                Player owner = plugin.getServer().getPlayer(grave.owner());
                if (owner != null) {
                    locale.send(owner, "grave.now-public",
                            Placeholder.component("coords", coords(grave.key())));
                }
            }
            if (grave.isExpired(now, cfg.expireMs())) {
                expire(grave);
                continue;
            }
            if (orphanPass) checkOrphan(grave);
        }
    }

    /** block gone but grave still tracked (fell before 0.1.1, /setblock, whatever): spill and forget */
    private void checkOrphan(Grave grave) {
        World world = plugin.getServer().getWorld(grave.key().worldId());
        if (world == null) return;
        if (!world.isChunkLoaded(grave.key().x() >> 4, grave.key().z() >> 4)) return;
        Location loc = grave.key().toLocation(world);
        sched.atBlock(loc, () -> {
            Block block = world.getBlockAt(loc);
            if (block.getType() == grave.placedAs()) return; // still standing, all good
            if (!graves.containsKey(grave.key())) return; // finished meanwhile
            dumpAll(grave, block);
        });
    }

    private void expire(Grave grave) {
        if (graves.remove(grave.key()) == null) return; // lost the race, fine
        World world = plugin.getServer().getWorld(grave.key().worldId());
        if (world != null) {
            Location loc = grave.key().toLocation(world);
            // we're on the global thread, block edits belong to the region
            sched.atBlock(loc, () -> {
                Block block = world.getBlockAt(loc);
                if (block.getType() != grave.placedAs()) return; // someone replaced it
                block.setType(Material.AIR);
                if (cfg.dropOnExpire()) {
                    List<ItemStack> everything = new ArrayList<>();
                    for (Portion p : grave.portions()) everything.addAll(p.items());
                    dropItems(block, everything);
                    int xp = grave.takeXp();
                    if (xp > 0) {
                        ExperienceOrb orb = world.spawn(
                                block.getLocation().add(0.5, 1.0, 0.5), ExperienceOrb.class);
                        orb.setExperience(xp);
                    }
                }
            });
        }
        Player owner = plugin.getServer().getPlayer(grave.owner());
        if (owner != null) {
            locale.send(owner, "grave.expired",
                    Placeholder.component("coords", coords(grave.key())));
        }
        persist();
    }

    // -- persistence --

    /** serialize on this thread, push the file write off-thread (folia: io is async-only) */
    public void persist() {
        // TODO if toYaml ever throws CME because a region thread brushed mid-iteration,
        // snapshot under a lock. hasn't happened in practice yet
        YamlConfiguration yaml = store.toYaml(graves.values());
        sched.async(() -> store.write(yaml));
    }

    /** shutdown path: async scheduler is already gone at this point */
    public void persistSync() {
        store.write(store.toYaml(graves.values()));
    }

    public void restore(Collection<Grave> loaded) {
        for (Grave g : loaded) graves.put(g.key(), g);
        if (!loaded.isEmpty()) plugin.getLogger().info("restored " + loaded.size() + " grave(s)");
    }

    // -- chat bits --

    /** clickable "x y z", click copies to clipboard */
    public Component coords(BlockKey key) {
        String text = key.x() + " " + key.y() + " " + key.z();
        return Component.text(text, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.copyToClipboard(text))
                .hoverEvent(HoverEvent.showText(locale.get("chat.click-to-copy")));
    }
}
