package dev.wndrxz.gravedig.command;

import java.util.List;
import java.util.Locale;
import dev.wndrxz.gravedig.GraveDig;
import dev.wndrxz.gravedig.grave.Grave;
import dev.wndrxz.gravedig.locale.LocaleManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class GraveDigCommand implements CommandExecutor, TabCompleter {

    private final GraveDig plugin;

    public GraveDigCommand(GraveDig plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        LocaleManager locale = plugin.locale();
        if (args.length == 0) {
            locale.send(sender, "command.usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("gravedig.admin")) {
                    locale.send(sender, "command.no-permission");
                    return true;
                }
                plugin.reloadEverything();
                locale.send(sender, "command.reloaded");
            }
            case "list" -> {
                if (!(sender instanceof Player player)) {
                    locale.send(sender, "command.usage");
                    return true;
                }
                sendList(player);
            }
            default -> locale.send(sender, "command.usage");
        }
        return true;
    }

    private void sendList(Player player) {
        LocaleManager locale = plugin.locale();
        List<Grave> mine = plugin.graves().byOwner(player.getUniqueId());
        if (mine.isEmpty()) {
            locale.send(player, "command.list-empty");
            return;
        }
        locale.send(player, "command.list-header");
        for (Grave g : mine) {
            locale.send(player, "command.list-entry",
                    Placeholder.component("coords", plugin.graves().coords(g.key())),
                    Placeholder.unparsed("portions", String.valueOf(g.portionsLeft())));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
