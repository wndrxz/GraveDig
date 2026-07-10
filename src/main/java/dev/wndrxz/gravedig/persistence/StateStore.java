package dev.wndrxz.gravedig.persistence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import dev.wndrxz.gravedig.grave.Grave;
import dev.wndrxz.gravedig.grave.Portion;
import dev.wndrxz.gravedig.util.BlockKey;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * graves survive restarts via state.yml, same idea as potioncombine's store.
 * itemstacks ride on bukkit's yaml serialization, no custom codecs.
 */
public final class StateStore {

    private final JavaPlugin plugin;
    private final File file;

    public StateStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "state.yml");
    }

    /** build yaml on the calling (region/global) thread, write() can go async */
    public YamlConfiguration toYaml(Collection<Grave> graves) {
        YamlConfiguration yaml = new YamlConfiguration();
        int i = 0;
        for (Grave g : graves) {
            ConfigurationSection s = yaml.createSection("graves." + i++);
            s.set("world", g.key().worldId().toString());
            s.set("x", g.key().x());
            s.set("y", g.key().y());
            s.set("z", g.key().z());
            s.set("owner", g.owner().toString());
            s.set("owner-name", g.ownerName());
            s.set("created", g.createdAtMs());
            s.set("placed-as", g.placedAs().name());
            s.set("xp", g.xp());
            int p = 0;
            for (Portion portion : g.portions()) {
                ConfigurationSection ps = s.createSection("portions." + p++);
                ps.set("type", portion.type().name());
                ps.set("items", new ArrayList<>(portion.items()));
            }
        }
        return yaml;
    }

    public void write(YamlConfiguration yaml) {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("failed to save state.yml: " + e.getMessage());
        }
    }

    /** boot-time read. sync on purpose: nothing else is running yet */
    public List<Grave> load() {
        List<Grave> out = new ArrayList<>();
        if (!file.exists()) return out;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection graves = yaml.getConfigurationSection("graves");
        if (graves == null) return out;
        for (String id : graves.getKeys(false)) {
            try {
                out.add(readGrave(graves.getConfigurationSection(id)));
            } catch (Exception e) {
                // one broken entry shouldn't nuke the whole file
                plugin.getLogger().warning("skipping bad grave entry " + id + ": " + e.getMessage());
            }
        }
        return out;
    }

    private Grave readGrave(ConfigurationSection s) {
        BlockKey key = new BlockKey(UUID.fromString(s.getString("world")),
                s.getInt("x"), s.getInt("y"), s.getInt("z"));
        List<Portion> portions = new ArrayList<>();
        ConfigurationSection all = s.getConfigurationSection("portions");
        if (all != null) {
            for (String pid : all.getKeys(false)) {
                ConfigurationSection p = all.getConfigurationSection(pid);
                Portion.Type type = Portion.Type.valueOf(
                        p.getString("type", "REST").toUpperCase(Locale.ROOT));
                List<ItemStack> items = new ArrayList<>();
                for (Object o : p.getList("items", List.of())) {
                    if (o instanceof ItemStack it) items.add(it);
                }
                portions.add(new Portion(type, items));
            }
        }
        return new Grave(key, UUID.fromString(s.getString("owner")),
                s.getString("owner-name", "?"), s.getLong("created"),
                Material.valueOf(s.getString("placed-as", "SUSPICIOUS_GRAVEL")),
                portions, s.getInt("xp"));
    }
}
