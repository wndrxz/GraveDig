package dev.wndrxz.gravedig;

import dev.wndrxz.gravedig.command.GraveDigCommand;
import dev.wndrxz.gravedig.config.ConfigManager;
import dev.wndrxz.gravedig.grave.GraveManager;
import dev.wndrxz.gravedig.listener.DeathListener;
import dev.wndrxz.gravedig.listener.DigListener;
import dev.wndrxz.gravedig.locale.LocaleManager;
import dev.wndrxz.gravedig.persistence.StateStore;
import dev.wndrxz.gravedig.sched.Sched;
import org.bukkit.plugin.java.JavaPlugin;

public final class GraveDig extends JavaPlugin {

    private ConfigManager config;
    private LocaleManager locale;
    private Sched sched;
    private StateStore store;
    private GraveManager graves;

    @Override
    public void onEnable() {
        sched = new Sched(this);
        config = new ConfigManager(this);
        config.load(LocaleManager.SHIPPED);
        locale = new LocaleManager(this);
        locale.load(config.locale());

        store = new StateStore(this);
        graves = new GraveManager(this, config, locale, sched, store);
        graves.restore(store.load()); // sync read at boot, nothing else runs yet

        getServer().getPluginManager().registerEvents(new DeathListener(config, graves), this);
        getServer().getPluginManager().registerEvents(new DigListener(config, locale, graves), this);

        GraveDigCommand cmd = new GraveDigCommand(this);
        getCommand("gravedig").setExecutor(cmd);
        getCommand("gravedig").setTabCompleter(cmd);

        // once a second is plenty for minute-scale timers
        sched.repeatGlobal(20L, graves::sweep);

        getLogger().info(locale.plain("startup.enabled")
                .replace("<locale>", config.locale().getLanguage()));
    }

    @Override
    public void onDisable() {
        // sync write on purpose: the async scheduler is already shut down here
        if (graves != null) graves.persistSync();
        if (locale != null) getLogger().info(locale.plain("startup.disabled"));
    }

    /** /gravedig reload: config + locales. graves stay as they are */
    public void reloadEverything() {
        config.load(LocaleManager.SHIPPED);
        locale.load(config.locale());
    }

    public ConfigManager config() {
        return config;
    }

    public LocaleManager locale() {
        return locale;
    }

    public GraveManager graves() {
        return graves;
    }
}
