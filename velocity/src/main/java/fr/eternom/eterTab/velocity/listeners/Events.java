package fr.eternom.eterTab.velocity.listeners;

import com.velocitypowered.api.proxy.ProxyServer;
import fr.eternom.eterTab.velocity.module.tab.TabListener;
import fr.eternom.eterTab.velocity.module.tab.TabService;

public class Events {

    public Events(Object plugin, ProxyServer proxy, TabService tab) {
        proxy.getEventManager().register(plugin, new TabListener(plugin, proxy, tab));
    }
}
