package fr.eternom.eterTab.velocity.module.tab;

import fr.eternom.eterTab.velocity.module.tab.Ranks.Rank;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.eternom.eterVelocityLib.core.Config;
import fr.eternom.eterTab.common.Animations;
import fr.eternom.eterVelocityLib.helper.Messages;
import fr.eternom.eterTab.velocity.module.server.ServerNames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Liste Tab de TOUT le réseau : chaque joueur voit tous les joueurs connectés au proxy, quel que soit leur serveur,
 * avec leur grade et les étiquettes posées par les plugins Paper (<tag_nom>, ex : métier), triés par poids de grade,
 * plus un en-tête et un pied dans sa langue.
 *
 * Les serveurs Paper envoient eux-mêmes leurs joueurs à la liste (et la réinitialisent au changement de serveur) :
 * on complète avec les joueurs des autres serveurs et on réécrit noms et ordre, à chaque rafraîchissement.
 */
public class TabService {

    /** Voir les joueurs invisibles (même permission que sur Paper). */
    public static final String VANISH_SEE = "eter.vanish.see";

    private final ProxyServer proxy;
    private final Messages messages;
    private final Animations animations;
    private final Ranks ranks;
    private final String playerFormat;
    private final String otherServerFormat;
    private final boolean sortByRank;
    private final ServerNames serverNames;
    private final TabTags tabTags;
    private final ZoneId zone;
    private final DateTimeFormatter timeFormat;
    private final DateTimeFormatter dateFormat;
    /** Joueurs que NOUS avons mis dans les listes : les seuls qu'on retire (pas les PNJ d'autres plugins). */
    private final Set<UUID> managed = ConcurrentHashMap.newKeySet();

    public TabService(ProxyServer proxy, Messages messages, Animations animations, Ranks ranks, ServerNames serverNames, TabTags tabTags,
                      Config config) {
        this.proxy = proxy;
        this.messages = messages;
        this.animations = animations;
        this.ranks = ranks;
        this.playerFormat = config.getString("tab.player-format", "<prefix><white><name></white><suffix>");
        this.otherServerFormat = config.getString("tab.player-format-other-server", playerFormat);
        this.sortByRank = config.getBoolean("tab.sort-by-rank", true);
        this.serverNames = serverNames;
        this.tabTags = tabTags;
        this.zone = ZoneId.of(config.getString("time-zone", "Europe/Paris"));
        this.timeFormat = DateTimeFormatter.ofPattern(config.getString("time-format", "HH:mm"));
        this.dateFormat = DateTimeFormatter.ofPattern(config.getString("date-format", "dd/MM/yyyy"));
    }

    /** Rafraîchit la liste et l'en-tête/pied de tout le monde (tâche répétée, et après connexion/changement de serveur). */
    public synchronized void refreshAll() {
        Collection<Player> players = List.copyOf(proxy.getAllPlayers());
        TagResolver animation = animations.current();

        // Deux versions du nom de chaque joueur : vu depuis son serveur, et vu depuis un autre (avec le serveur affiché)
        Map<UUID, Component> sameServerNames = new HashMap<>();
        Map<UUID, Component> otherServerNames = new HashMap<>();
        Map<UUID, Integer> orders = new HashMap<>();
        for (Player target : players) {
            // Un badge posé par un plugin Paper (tag de clan...) remplace le grade ; l'ordre du Tab reste celui du grade
            String badge = tabTags.of(target.getUniqueId()).get("badge");
            Rank rank = ranks.of(target.getUniqueId())
                    .withBadge(badge == null || badge.isBlank() ? null : messages.render(badge, TagResolver.empty()));
            TagResolver tags = TagResolver.resolver(animation,
                    Placeholder.component("prefix", rank.prefix()),
                    Placeholder.component("suffix", rank.suffix()),
                    Placeholder.unparsed("name", target.getUsername()),
                    Placeholder.unparsed("group", rank.group()),
                    Placeholder.unparsed("server", displayName(serverName(target))),
                    tags(target.getUniqueId()));
            sameServerNames.put(target.getUniqueId(), messages.render(playerFormat, tags));
            otherServerNames.put(target.getUniqueId(), messages.render(otherServerFormat, tags));
            orders.put(target.getUniqueId(), sortByRank ? rank.weight() : 0);
        }

        Set<UUID> online = orders.keySet();
        // Les invisibles (vanish du staff, étiquette posée par EterLib) ne comptent pas et ne sont montrés qu'au staff
        List<Player> visible = players.stream().filter(player -> !isVanished(player)).toList();
        for (Player viewer : players) {
            TabList list = viewer.getTabList();
            String viewerServer = serverName(viewer);
            boolean seesVanished = viewer.hasPermission(VANISH_SEE);
            for (Player target : players) {
                if (target != viewer && !seesVanished && isVanished(target)) {
                    list.removeEntry(target.getUniqueId());
                    continue;
                }
                Component name = serverName(target).equals(viewerServer)
                        ? sameServerNames.get(target.getUniqueId())
                        : otherServerNames.get(target.getUniqueId());
                upsert(list, target, name, orders.get(target.getUniqueId()));
            }
            // Joueurs partis du réseau : seulement ceux qu'on a ajoutés nous-mêmes
            for (TabListEntry entry : List.copyOf(list.getEntries())) {
                UUID id = entry.getProfile().getId();
                if (managed.contains(id) && !online.contains(id)) {
                    list.removeEntry(id);
                }
            }
            updateHeaderFooter(viewer, visible, animation);
        }
        managed.retainAll(online);
        managed.addAll(online);
    }

    /** Invisible (vanish du staff) : étiquette « vanished » posée par EterLib. */
    public boolean isVanished(Player player) {
        String vanished = tabTags.of(player.getUniqueId()).get("vanished");
        return vanished != null && !vanished.isEmpty();
    }

    /** Retire tout de suite un joueur qui quitte le réseau des listes des autres. */
    public synchronized void remove(UUID player) {
        managed.remove(player);
        proxy.getAllPlayers().forEach(viewer -> viewer.getTabList().removeEntry(player));
    }

    private void upsert(TabList list, Player target, Component name, int order) {
        int latency = (int) Math.min(Integer.MAX_VALUE, target.getPing());
        TabListEntry entry = list.getEntry(target.getUniqueId()).orElse(null);
        if (entry == null) {
            // Joueur d'un autre serveur : son profil (donc son skin) vient du proxy
            list.addEntry(TabListEntry.builder()
                    .tabList(list)
                    .profile(target.getGameProfile())
                    .displayName(name)
                    .latency(latency)
                    .listed(true)
                    .listOrder(order)
                    .build());
            return;
        }
        if (!Objects.equals(entry.getDisplayNameComponent().orElse(null), name)) {
            entry.setDisplayName(name);
        }
        if (entry.getListOrder() != order) {
            entry.setListOrder(order);
        }
        if (entry.getLatency() != latency) {
            entry.setLatency(latency);
        }
    }

    private void updateHeaderFooter(Player viewer, Collection<Player> players, TagResolver animation) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        String server = serverName(viewer);
        long serverOnline = players.stream().filter(player -> serverName(player).equals(server)).count();
        TagResolver tags = TagResolver.resolver(animation,
                Placeholder.unparsed("player", viewer.getUsername()),
                Placeholder.unparsed("online", String.valueOf(players.size())),
                Placeholder.unparsed("max", String.valueOf(proxy.getConfiguration().getShowMaxPlayers())),
                Placeholder.unparsed("server", displayName(server)),
                Placeholder.unparsed("server_online", String.valueOf(serverOnline)),
                Placeholder.unparsed("ping", String.valueOf(viewer.getPing())),
                Placeholder.unparsed("time", timeFormat.format(now)),
                Placeholder.unparsed("date", dateFormat.format(now)),
                Placeholder.component("servers", servers(viewer, players, animation)));
        viewer.sendPlayerListHeaderAndFooter(messages.get(viewer, "tab.header", tags), messages.get(viewer, "tab.footer", tags));
    }

    /** « Survie 3 · Ressources 1 » : chaque serveur qui a des joueurs, au format de tab.server-entry. */
    private Component servers(Player viewer, Collection<Player> players, TagResolver animation) {
        List<Component> entries = new ArrayList<>();
        for (RegisteredServer registered : proxy.getAllServers()) {
            String name = registered.getServerInfo().getName();
            long count = players.stream().filter(player -> serverName(player).equals(name)).count();
            if (count == 0) {
                continue; // serveurs vides non listés : le pied reste court
            }
            entries.add(messages.get(viewer, "tab.server-entry", TagResolver.resolver(animation,
                    Placeholder.unparsed("name", displayName(name)),
                    Placeholder.unparsed("count", String.valueOf(count)))));
        }
        return Component.join(JoinConfiguration.separator(messages.get(viewer, "tab.server-separator", TagResolver.empty())), entries);
    }

    /**
     * <tag_nom> : les étiquettes posées par les plugins Paper pour ce joueur (TabTags) ; vide si absente, pour qu'un
     * format qui en cite une reste propre sur un serveur ou pour un joueur qui ne l'a pas.
     */
    private TagResolver tags(UUID player) {
        Map<String, Component> rendered = new HashMap<>();
        tabTags.of(player).forEach((name, value) -> rendered.put(name, messages.render(value, TagResolver.empty())));
        return new TagResolver() {
            @Override
            public Tag resolve(String name, ArgumentQueue arguments, Context context) {
                return has(name) ? Tag.selfClosingInserting(rendered.getOrDefault(name.substring(4), Component.empty())) : null;
            }

            @Override
            public boolean has(String name) {
                return name.startsWith("tag_");
            }
        };
    }

    /** Nom affiché d'un serveur (donné par le serveur lui-même), sinon son nom dans velocity.toml. */
    private String displayName(String server) {
        return serverNames.displayName(server);
    }

    private static String serverName(Player player) {
        return player.getCurrentServer().map(ServerConnection::getServerInfo).map(info -> info.getName()).orElse("?");
    }
}
