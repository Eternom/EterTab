package fr.eternom.eterTab.velocity.listeners;

import fr.eternom.eterTab.velocity.EterTabVelocity;
import fr.eternom.eterTab.velocity.module.command.EterTabCommand;
import fr.eternom.eterTab.velocity.module.motd.MotdListener;
import fr.eternom.eterTab.velocity.module.tab.TabListener;

public class Events {

    public Events(EterTabVelocity plugin) {
        plugin.proxy().getEventManager().register(plugin, new TabListener(plugin));
        plugin.proxy().getEventManager().register(plugin, new MotdListener(plugin));
        plugin.proxy().getCommandManager().register(
                plugin.proxy().getCommandManager().metaBuilder("etertab").plugin(plugin).build(),
                new EterTabCommand(plugin));
    }
}
