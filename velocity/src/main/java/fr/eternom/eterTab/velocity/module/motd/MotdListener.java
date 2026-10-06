package fr.eternom.eterTab.velocity.module.motd;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.proxy.Player;
import fr.eternom.eterTab.velocity.EterTabVelocity;
import fr.eternom.eterTab.velocity.module.maintenance.Maintenance;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/** MOTD de la liste des serveurs, et refus des connexions pendant la maintenance. */
public class MotdListener {

    private final EterTabVelocity plugin;

    public MotdListener(EterTabVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPing(ProxyPingEvent event) {
        event.setPing(plugin.motd().apply(event.getPing()));
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        if (plugin.maintenance().isEnabled() && !player.hasPermission(Maintenance.BYPASS)) {
            event.setResult(ComponentResult.denied(plugin.messages().get(player, "maintenance.kick", TagResolver.empty())));
        }
    }
}
