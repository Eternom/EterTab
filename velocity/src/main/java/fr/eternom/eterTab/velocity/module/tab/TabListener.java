package fr.eternom.eterTab.velocity.module.tab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import fr.eternom.eterTab.velocity.EterTabVelocity;

import java.time.Duration;

public class TabListener {

    /** Laisse au serveur Paper le temps d'envoyer sa propre liste avant qu'on la complète. */
    private static final Duration AFTER_CONNECT = Duration.ofMillis(500);

    private final EterTabVelocity plugin;

    public TabListener(EterTabVelocity plugin) {
        this.plugin = plugin;
    }

    /** Arrivée sur le réseau ou changement de serveur : le serveur Paper vient de réinitialiser la liste du joueur. */
    @Subscribe
    public void onServerConnected(ServerPostConnectEvent event) {
        plugin.proxy().getScheduler().buildTask(plugin, () -> plugin.tab().refreshAll()).delay(AFTER_CONNECT).schedule();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        plugin.tab().remove(event.getPlayer().getUniqueId());
    }
}
