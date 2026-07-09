package dev.wndrxz.gravedig.grave;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import dev.wndrxz.gravedig.util.BlockKey;
import org.bukkit.Material;

/** mutable grave state, one per placed block */
public final class Grave {

    private final BlockKey key;
    private final UUID owner;
    private final String ownerName;
    private final long createdAtMs;
    private final Material placedAs;
    private final Deque<Portion> portions;
    private int xp;
    private int clicksIntoPortion;
    private boolean publicAnnounced; // so we nag the owner only once

    public Grave(BlockKey key, UUID owner, String ownerName, long createdAtMs,
                 Material placedAs, List<Portion> portions, int xp) {
        this.key = key;
        this.owner = owner;
        this.ownerName = ownerName;
        this.createdAtMs = createdAtMs;
        this.placedAs = placedAs;
        this.portions = new ArrayDeque<>(portions);
        this.xp = xp;
    }

    public BlockKey key() { return key; }
    public UUID owner() { return owner; }
    public String ownerName() { return ownerName; }
    public long createdAtMs() { return createdAtMs; }
    public Material placedAs() { return placedAs; }
    public int xp() { return xp; }
    public int portionsLeft() { return portions.size(); }
    public Deque<Portion> portions() { return portions; }

    /** counts a brush click, pops the portion when enough. null = not done yet */
    public Portion brushClick(int clicksPerPortion) {
        if (portions.isEmpty()) return null;
        clicksIntoPortion++;
        if (clicksIntoPortion < clicksPerPortion) return null;
        clicksIntoPortion = 0;
        return portions.poll();
    }

    public int takeXp() {
        int v = xp;
        xp = 0;
        return v;
    }

    public boolean isEmpty() { return portions.isEmpty() && xp <= 0; }

    public boolean isProtectedFrom(UUID digger, long nowMs, long protectMs) {
        if (protectMs <= 0) return false;
        if (digger.equals(owner)) return false;
        return nowMs - createdAtMs < protectMs;
    }

    public boolean isExpired(long nowMs, long expireMs) {
        return expireMs > 0 && nowMs - createdAtMs >= expireMs;
    }

    /** true exactly once, when the protection window has just run out */
    public boolean protectionJustEnded(long nowMs, long protectMs) {
        if (publicAnnounced || protectMs <= 0) return false;
        if (nowMs - createdAtMs >= protectMs) {
            publicAnnounced = true;
            return true;
        }
        return false;
    }
}
