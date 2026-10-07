package fr.eternom.eterTab.velocity.listeners;

import fr.eternom.eterTab.velocity.EterTabVelocity;
import fr.eternom.eterTab.velocity.module.command.EterTabCommand;
import fr.eternom.eterTab.velocity.module.motd.MotdListener;
import fr.eternom.eterTab.velocity.module.server.ServerNameListener;
import fr.eternom.eterTab.velocity.module.tab.TabTags;
import fr.eternom.eterTab.velocity.module.tab.TabListener;

public class Events {

    public Events(EterTabVelocity plugin) {
        plugin.proxy().getEventManager().register(plugin, new TabListener(plugin));
        plugin.proxy().getEventManager().register(plugin, new MotdListener(plugin));
        plugin.proxy().getChannelRegistrar().register(ServerNameListener.CHANNEL);
        plugin.proxy().getEventManager().register(plugin, new ServerNameListener(plugin.serverNames()));
        plugin.proxy().getChannelRegistrar().register(TabTags.CHANNEL);
        plugin.proxy().getEventManager().register(plugin, plugin.tabTags());
        plugin.proxy().getCommandManager().register(
                plugin.proxy().getCommandManager().metaBuilder("etertab").plugin(plugin).build(),
                new EterTabCommand(plugin));
    }
}
