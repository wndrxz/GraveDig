package dev.wndrxz.gravedig.sched;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

/**
 * thin wrapper over paper's folia-aware schedulers. region stuff for block
 * edits, async for disk io. no BukkitScheduler anywhere -- it throws on folia.
 */
public final class Sched {

    private final Plugin plugin;

    public Sched(Plugin plugin) {
        this.plugin = plugin;
    }

    /** run on the region that owns this location (block edits go here) */
    public void atBlock(Location loc, Runnable task) {
        plugin.getServer().getRegionScheduler().execute(plugin, loc, task);
    }

    /** off-thread, for file io only */
    public void async(Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    /** global repeating tick task (expiry sweep lives on this) */
    public void repeatGlobal(long everyTicks, Runnable task) {
        plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, t -> task.run(), everyTicks, everyTicks);
    }
}
