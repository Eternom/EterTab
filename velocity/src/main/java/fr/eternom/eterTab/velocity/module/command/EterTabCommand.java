package fr.eternom.eterTab.velocity.module.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import fr.eternom.eterTab.velocity.EterTabVelocity;
import fr.eternom.eterTab.velocity.module.maintenance.Maintenance;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * /etertab reload : relit config et langues sans redémarrer le proxy.
 * /etertab maintenance on|off|status : mode maintenance (les joueurs sans etertab.maintenance.bypass sont renvoyés).
 */
public class EterTabCommand implements SimpleCommand {

    public static final String PERMISSION = "etertab.admin";

    private final EterTabVelocity plugin;

    public EterTabCommand(EterTabVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reload(source);
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("maintenance")) {
            maintenance(source, args.length == 2 ? args[1].toLowerCase(Locale.ROOT) : "status");
        } else {
            plugin.messages().send(source, "command.usage", TagResolver.empty());
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            return filter(List.of("reload", "maintenance"), args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("maintenance")) {
            return filter(List.of("on", "off", "status"), args[1]);
        }
        return List.of();
    }

    private void reload(CommandSource source) {
        if (plugin.load()) {
            plugin.messages().send(source, "command.reloaded", TagResolver.empty());
        } else {
            plugin.messages().send(source, "command.reload-failed", TagResolver.empty());
        }
    }

    private void maintenance(CommandSource source, String action) {
        Maintenance maintenance = plugin.maintenance();
        try {
            switch (action) {
                case "on" -> {
                    maintenance.set(true);
                    // Les joueurs déjà connectés sans autorisation sont renvoyés
                    plugin.proxy().getAllPlayers().stream()
                            .filter(player -> !player.hasPermission(Maintenance.BYPASS))
                            .forEach(player -> player.disconnect(plugin.messages().get(player, "maintenance.kick", TagResolver.empty())));
                    plugin.messages().send(source, "maintenance.enabled", TagResolver.empty());
                }
                case "off" -> {
                    maintenance.set(false);
                    plugin.messages().send(source, "maintenance.disabled", TagResolver.empty());
                }
                default -> plugin.messages().send(source,
                        maintenance.isEnabled() ? "maintenance.status-on" : "maintenance.status-off", TagResolver.empty());
            }
        } catch (IOException e) {
            plugin.messages().send(source, "maintenance.save-failed", TagResolver.empty());
        }
    }

    private static List<String> filter(List<String> options, String prefix) {
        String start = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(start)).toList();
    }
}
