package fr.eternom.eterTab.velocity.module.tab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;

import java.time.Duration;

public class TabListener {

    /** Laisse au serveur Paper le temps d'envoyer sa propre liste avant qu'on la complète. */
    private static final Duration AFTER_CONNECT = Duration.ofMillis(500);

    private final Object plugin;
    private final ProxyServer proxy;
    private final TabService tab;

    public TabListener(Object plugin, ProxyServer proxy, TabService tab) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.tab = tab;
    }

    /** Arrivée sur le réseau ou changement de serveur : le serveur Paper vient de réinitialiser la liste du joueur. */
    @Subscribe
    public void onServerConnected(ServerPostConnectEvent event) {
        proxy.getScheduler().buildTask(plugin, tab::refreshAll).delay(AFTER_CONNECT).schedule();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        tab.remove(event.getPlayer().getUniqueId());
    }
}
