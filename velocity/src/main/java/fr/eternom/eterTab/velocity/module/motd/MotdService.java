package fr.eternom.eterTab.velocity.module.motd;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import fr.eternom.eterVelocityLib.core.Config;
import fr.eternom.eterTab.common.Animations;
import fr.eternom.eterVelocityLib.helper.Messages;
import fr.eternom.eterTab.velocity.module.maintenance.Maintenance;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Réponse au ping de la liste des serveurs du jeu : texte sur deux lignes (plusieurs au choix, au hasard ou à tour
 * de rôle), joueurs en ligne et maximum, lignes au survol du nombre de joueurs, texte de version, icône.
 * En maintenance : textes dédiés, et le texte de version remplace les barres de signal.
 *
 * Les textes sont dans config.yml, pas dans lang/ : le jeu n'envoie pas sa langue quand il interroge un serveur.
 */
public class MotdService {

    /** UUID vide pour les lignes de survol qui ne sont pas de vrais joueurs. */
    private static final UUID NO_PLAYER = new UUID(0, 0);

    private final ProxyServer proxy;
    /** Les invisibles (vanish du staff) : ni comptés ni montrés. */
    private final Predicate<Player> hidden;
    private final Messages messages;
    private final Animations animations;
    private final Maintenance maintenance;

    private final boolean enabled;
    private final boolean rotate;
    private final long rotateMillis;
    private final List<String> motds;
    private final int maxPlayers;
    private final List<String> hover;
    private final boolean hoverShowPlayers;
    private final int hoverMaxPlayers;
    private final String hoverPlayer;
    private final String hoverMore;
    private final String versionName;
    private final Favicon favicon;

    private final List<String> maintenanceMotds;
    private final List<String> maintenanceHover;
    private final String maintenanceVersion;

    public MotdService(ProxyServer proxy, Messages messages, Animations animations, Maintenance maintenance, Config config,
                       Path dataDirectory, Logger logger, Predicate<Player> hidden) {
        this.proxy = proxy;
        this.hidden = hidden;
        this.messages = messages;
        this.animations = animations;
        this.maintenance = maintenance;

        this.enabled = config.getBoolean("motd.enabled", true);
        this.rotate = "rotate".equalsIgnoreCase(config.getString("motd.mode", "random"));
        this.rotateMillis = Math.max(1, config.getInt("motd.rotate-seconds", 10)) * 1000L;
        this.motds = config.getStringList("motd.messages");
        this.maxPlayers = config.getInt("motd.max-players", 0);
        this.hover = config.getStringList("motd.hover");
        this.hoverShowPlayers = config.getBoolean("motd.hover-show-players", true);
        this.hoverMaxPlayers = Math.max(0, config.getInt("motd.hover-max-players", 10));
        this.hoverPlayer = config.getString("motd.hover-player", "<info>• <white><name>");
        this.hoverMore = config.getString("motd.hover-more", "<info>... et <accent><count></accent> autres");
        this.versionName = config.getString("motd.version-name", "");
        this.favicon = loadFavicon(dataDirectory, config.getString("motd.favicon", ""), logger);

        this.maintenanceMotds = config.getStringList("maintenance.motd");
        this.maintenanceHover = config.getStringList("maintenance.hover");
        this.maintenanceVersion = config.getString("maintenance.version-name", "<error>Maintenance");
    }

    /** Remplace la réponse de Velocity par la nôtre (sans toucher à ce qui n'est pas configuré). */
    public ServerPing apply(ServerPing original) {
        if (!enabled) {
            return original;
        }
        boolean inMaintenance = maintenance.isEnabled();
        int online = (int) proxy.getAllPlayers().stream().filter(hidden.negate()).count();
        int max = maxPlayers > 0 ? maxPlayers : online + 1;
        TagResolver tags = TagResolver.resolver(animations.current(),
                Placeholder.unparsed("online", String.valueOf(online)),
                Placeholder.unparsed("max", String.valueOf(max)));

        ServerPing.Builder ping = original.asBuilder().onlinePlayers(online).maximumPlayers(max);

        List<String> texts = inMaintenance ? maintenanceMotds : motds;
        if (!texts.isEmpty()) {
            ping.description(messages.render(pick(texts), tags));
        }

        ping.clearSamplePlayers().samplePlayers(hoverLines(inMaintenance, tags));

        String version = inMaintenance ? maintenanceVersion : versionName;
        if (!version.isEmpty()) {
            // Protocole -1 en maintenance : le jeu affiche notre texte à la place des barres de signal
            int protocol = inMaintenance ? -1 : original.getVersion().getProtocol();
            ping.version(new ServerPing.Version(protocol, messages.legacy(version, tags)));
        }
        if (favicon != null) {
            ping.favicon(favicon);
        }
        return ping.build();
    }

    /** Lignes au survol du nombre de joueurs : celles de la config, puis les joueurs connectés (hors maintenance). */
    private List<ServerPing.SamplePlayer> hoverLines(boolean inMaintenance, TagResolver tags) {
        List<ServerPing.SamplePlayer> lines = new ArrayList<>();
        for (String line : inMaintenance ? maintenanceHover : hover) {
            lines.add(new ServerPing.SamplePlayer(messages.legacy(line, tags), NO_PLAYER));
        }
        if (inMaintenance || !hoverShowPlayers || hoverMaxPlayers == 0) {
            return lines;
        }
        List<Player> players = proxy.getAllPlayers().stream().filter(hidden.negate()).toList();
        for (Player player : players.subList(0, Math.min(hoverMaxPlayers, players.size()))) {
            lines.add(new ServerPing.SamplePlayer(messages.legacy(hoverPlayer,
                    TagResolver.resolver(tags, Placeholder.unparsed("name", player.getUsername()))), player.getUniqueId()));
        }
        if (players.size() > hoverMaxPlayers) {
            lines.add(new ServerPing.SamplePlayer(messages.legacy(hoverMore,
                    TagResolver.resolver(tags, Placeholder.unparsed("count", String.valueOf(players.size() - hoverMaxPlayers)))), NO_PLAYER));
        }
        return lines;
    }

    private String pick(List<String> texts) {
        int index = rotate
                ? (int) ((System.currentTimeMillis() / rotateMillis) % texts.size())
                : ThreadLocalRandom.current().nextInt(texts.size());
        return texts.get(index);
    }

    /** Icône 64x64 en PNG dans plugins/etertab/ ; absente ou invalide : celle de Velocity est gardée. */
    private static Favicon loadFavicon(Path dataDirectory, String fileName, Logger logger) {
        if (fileName.isBlank()) {
            return null;
        }
        Path file = dataDirectory.resolve(fileName);
        if (!Files.exists(file)) {
            logger.warn("Icône du MOTD introuvable : {} (celle de Velocity est utilisée)", file);
            return null;
        }
        try {
            return Favicon.create(file);
        } catch (IOException | IllegalArgumentException e) {
            logger.warn("Icône du MOTD invalide (PNG 64x64 attendu) : {}", file, e);
            return null;
        }
    }
}
