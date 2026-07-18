package dev.wndrxz.gravedig.config;

import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** typed view over config.yml, potioncombine style: load once, getters after */
public final class ConfigManager {

    private final JavaPlugin plugin;

    private Locale locale;
    private boolean enabled;
    private Material graveBlock; // null = auto-pick by ground
    private long protectMs;
    private long expireMs;
    private boolean dropOnExpire;
    private boolean breakDropsAll;
    private int clicksPerPortion;
    private boolean digEffects;
    private boolean xpEnabled;
    private double xpPercent;
    private boolean deathCoords;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(Set<String> shippedLocales) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        locale = resolveLocale(c.getString("locale", "auto"), shippedLocales);
        enabled = c.getBoolean("grave.enabled", true);
        graveBlock = parseGraveBlock(c.getString("grave.block", "auto"));
        protectMs = minutesToMs(c.getInt("grave.protect-minutes", 15));
        expireMs = minutesToMs(c.getInt("grave.expire-minutes", 1440));
        dropOnExpire = c.getBoolean("grave.drop-on-expire", true);
        breakDropsAll = c.getBoolean("grave.break-drops-all", true);
        clicksPerPortion = Math.max(1, c.getInt("dig.clicks-per-portion", 5));
        digEffects = c.getBoolean("dig.effects", true);
        xpEnabled = c.getBoolean("xp.enabled", true);
        xpPercent = clamp01(c.getDouble("xp.percent", 1.0));
        deathCoords = c.getBoolean("messages.death-coords", true);
    }

    private static long minutesToMs(int minutes) {
        return minutes <= 0 ? 0 : minutes * 60_000L;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private Material parseGraveBlock(String raw) {
        if (raw == null || raw.equalsIgnoreCase("auto")) return null;
        Material m = Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
        if (m == Material.SUSPICIOUS_SAND || m == Material.SUSPICIOUS_GRAVEL) return m;
        plugin.getLogger().warning("grave.block '" + raw + "' is not brushable, using auto");
        return null;
    }

    private Locale resolveLocale(String raw, Set<String> shipped) {
        if (raw != null && !raw.equalsIgnoreCase("auto")) {
            String lang = raw.trim().toLowerCase(Locale.ROOT);
            if (shipped.contains(lang)) return Locale.of(lang);
            plugin.getLogger().warning("locale '" + raw + "' not shipped, using auto");
        }
        String def = Locale.getDefault().getLanguage();
        return shipped.contains(def) ? Locale.of(def) : Locale.ENGLISH;
    }

    public Locale locale() { return locale; }
    public boolean enabled() { return enabled; }
    public Material graveBlock() { return graveBlock; }
    public long protectMs() { return protectMs; }
    public long expireMs() { return expireMs; }
    public boolean dropOnExpire() { return dropOnExpire; }
    public boolean breakDropsAll() { return breakDropsAll; }
    public int clicksPerPortion() { return clicksPerPortion; }
    public boolean digEffects() { return digEffects; }
    public boolean xpEnabled() { return xpEnabled; }
    public double xpPercent() { return xpPercent; }
    public boolean deathCoords() { return deathCoords; }
}
