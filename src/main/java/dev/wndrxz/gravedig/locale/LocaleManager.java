package dev.wndrxz.gravedig.locale;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * yml messages with minimessage markup, same approach as potioncombine:
 * shipped lang files get copied to the data folder so admins can edit them,
 * missing keys fall back to en and get logged once.
 */
public final class LocaleManager {

    public static final Set<String> SHIPPED = Set.of("en", "ru");

    private final JavaPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Set<String> missingLogged = new HashSet<>();

    private YamlConfiguration messages;
    private YamlConfiguration fallback;
    private String prefixRaw = "";

    public LocaleManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(Locale locale) {
        String lang = SHIPPED.contains(locale.getLanguage()) ? locale.getLanguage() : "en";
        messages = readLang(lang);
        fallback = lang.equals("en") ? messages : readLang("en");
        prefixRaw = raw("prefix");
        missingLogged.clear();
    }

    public Component get(String key, TagResolver... resolvers) {
        return mm.deserialize(raw(key), resolvers);
    }

    public Component prefixed(String key, TagResolver... resolvers) {
        return mm.deserialize(prefixRaw + raw(key), resolvers);
    }

    public void send(Audience to, String key, TagResolver... resolvers) {
        to.sendMessage(prefixed(key, resolvers));
    }

    /** raw string, minimessage tags left as-is (logs, string templates) */
    public String plain(String key) {
        return raw(key);
    }

    private String raw(String key) {
        String v = messages != null ? messages.getString(key) : null;
        if (v == null && fallback != null) v = fallback.getString(key);
        if (v == null) {
            if (missingLogged.add(key)) plugin.getLogger().warning("missing locale key: " + key);
            return key;
        }
        return v;
    }

    private YamlConfiguration readLang(String lang) {
        File file = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!file.exists()) plugin.saveResource("lang/" + lang + ".yml", false);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        // bundled copy backs the user file so new keys still resolve after updates
        try (InputStream in = plugin.getResource("lang/" + lang + ".yml")) {
            if (in != null) {
                cfg.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("failed reading bundled lang " + lang + ": " + e.getMessage());
        }
        return cfg;
    }
}
