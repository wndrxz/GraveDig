package dev.wndrxz.gravedig.grave;

/**
 * vanilla xp curve, the piecewise 17/32 split from the wiki.
 * kept pure so junit covers it without a server.
 */
public final class XpMath {

    private XpMath() {}

    /** total points sitting at the start of this level */
    public static int totalAtLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    /** points needed to go from this level to the next */
    public static int toNextLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    /** total points for level + progress fraction into the next one */
    public static int totalPoints(int level, float progress) {
        float p = Math.max(0f, Math.min(1f, progress));
        return totalAtLevel(level) + Math.round(p * toNextLevel(level));
    }
}
