package fr.eternom.eterTab.velocity.module.server;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.nio.charset.StandardCharsets;

/**
 * Reçoit le nom affiché qu'un serveur Paper envoie (EterLib, canal eter:server).
 * Seul un serveur peut le donner, et seulement le sien : un message venant d'un joueur (client modifié) est ignoré.
 */
public class ServerNameListener {

    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("eter", "server");
    private static final MinecraftChannelIdentifier REGISTER = MinecraftChannelIdentifier.create("minecraft", "register");

    private final ServerNames names;

    public ServerNameListener(ServerNames names) {
        this.names = names;
    }

    /**
     * Paper n'envoie un message que sur un canal déclaré par l'autre bout : on le déclare au serveur à chaque connexion
     * d'un joueur, ce qui déclenche l'envoi du nom par EterLib.
     */
    @Subscribe
    public void onConnected(ServerPostConnectEvent event) {
        event.getPlayer().getCurrentServer().ifPresent(server ->
                server.sendPluginMessage(REGISTER, CHANNEL.getId().getBytes(StandardCharsets.UTF_8)));
    }

    @Subscribe
    public void onMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled()); // ne va ni au joueur ni au serveur
        if (!(event.getSource() instanceof ServerConnection server)) {
            return;
        }
        try {
            names.update(server.getServerInfo().getName(), event.dataAsDataStream().readUTF().trim());
        } catch (RuntimeException ignored) {
            // Message mal formé : on garde l'ancien nom
        }
    }
}
