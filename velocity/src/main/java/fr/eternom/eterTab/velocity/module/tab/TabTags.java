package fr.eternom.eterTab.velocity.module.tab;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Étiquettes des joueurs dans la liste Tab, posées par les plugins Paper via EterLib (canal eter:tab) : un nom et un
 * texte MiniMessage, affiché à la place de <tag_nom> dans tab.player-format (ex : <tag_job> = « · Mineur (2) »).
 * Gardées jusqu'à la déconnexion, même sur un serveur qui ne les pose pas (le lobby montre encore le métier).
 * Seul un serveur peut en poser, et seulement pour le joueur qui y est connecté : un message venant d'un joueur (client
 * modifié) est ignoré.
 */
public class TabTags {

    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("eter", "tab");
    private static final MinecraftChannelIdentifier REGISTER = MinecraftChannelIdentifier.create("minecraft", "register");
    private static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,32}");
    /** Un texte d'étiquette raisonnable : au-delà, ignoré (la liste Tab reste lisible). */
    private static final int MAX_LENGTH = 512;

    private final Map<UUID, Map<String, String>> tags = new ConcurrentHashMap<>();

    /** Étiquettes d'un joueur (nom -> MiniMessage). */
    public Map<String, String> of(UUID player) {
        return tags.getOrDefault(player, Map.of());
    }

    /** Paper n'envoie que sur un canal déclaré par l'autre bout : on le déclare au serveur à chaque connexion. */
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
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection server)) {
            return;
        }
        try {
            ByteArrayDataInput in = event.dataAsDataStream();
            String name = in.readUTF();
            String value = in.readUTF();
            if (!NAME.matcher(name).matches() || value.length() > MAX_LENGTH) {
                return;
            }
            Map<String, String> own = tags.computeIfAbsent(server.getPlayer().getUniqueId(), uuid -> new ConcurrentHashMap<>());
            if (value.isEmpty()) {
                own.remove(name);
            } else {
                own.put(name, value);
            }
        } catch (RuntimeException ignored) {
            // message mal formé
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        tags.remove(event.getPlayer().getUniqueId());
    }
}
