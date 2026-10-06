package fr.eternom.eterTab.paper.listeners;

import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterTab.paper.Main;
import fr.eternom.eterTab.paper.module.sidebar.SidebarCommand;
import fr.eternom.eterTab.paper.module.sidebar.SidebarPreferences;
import fr.eternom.eterTab.paper.module.sidebar.SidebarService;
import org.bukkit.command.PluginCommand;

import java.util.Objects;

public class Commands {

    public Commands(Main main, Messages messages, SidebarPreferences preferences, SidebarService sidebar) {
        PluginCommand command = Objects.requireNonNull(main.getCommand("sidebar"), "Commande absente du plugin.yml : sidebar");
        if (sidebar == null) {
            // Sidebar désactivée dans la config : la commande l'explique au lieu de ne rien faire
            command.setExecutor((sender, cmd, label, args) -> {
                messages.send(sender, "sidebar.unavailable");
                return true;
            });
        } else {
            command.setExecutor(new SidebarCommand(main, preferences, sidebar, messages));
        }
    }

}
