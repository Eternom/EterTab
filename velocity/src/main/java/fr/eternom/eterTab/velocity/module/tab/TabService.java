package fr.eternom.eterTab.velocity.module.tab;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.eternom.eterTab.velocity.core.Config;
import fr.eternom.eterTab.velocity.helper.Animations;
import fr.eternom.eterTab.velocity.helper.Messages;
import fr.eternom.eterTab.velocity.module.tab.Ranks.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
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
 * avec leur grade, triés par poids de grade, plus un en-tête et un pied dans sa langue.
 *
 * Les serveurs Paper envoient eux-mêmes leurs joueurs à la liste (et la réinitialisent au changement de serveur) :
 * on complète avec les joueurs des autres serveurs et on réécrit noms et ordre, à chaque rafraîchissement.
 */
public class TabService {

    private final ProxyServer proxy;
    private final Messages messages;
    private final Animations animations;
    private final Ranks ranks;
    private final String playerFormat;
    private final boolean sortByRank;
    private final ZoneId zone;
    private final DateTimeFormatter timeFormat;
    private final DateTimeFormatter dateFormat;
    /** Joueurs que NOUS avons mis dans les listes : les seuls qu'on retire (pas les PNJ d'autres plugins). */
    private final Set<UUID> managed = ConcurrentHashMap.newKeySet();

    public TabService(ProxyServer proxy, Messages messages, Animations animations, Ranks ranks, Config config) {
        this.proxy = proxy;
        this.messages = messages;
        this.animations = animations;
        this.ranks = ranks;
        this.playerFormat = config.getString("tab.player-format", "<prefix><white><name></white><suffix>");
        this.sortByRank = config.getBoolean("tab.sort-by-rank", true);
        this.zone = ZoneId.of(config.getString("time-zone", "Europe/Paris"));
        this.timeFormat = DateTimeFormatter.ofPattern(config.getString("time-format", "HH:mm"));
        this.dateFormat = DateTimeFormatter.ofPattern(config.getString("date-format", "dd/MM/yyyy"));
    }

    /** Rafraîchit la liste et l'en-tête/pied de tout le monde (tâche répétée, et après connexion/changement de serveur). */
    public synchronized void refreshAll() {
        Collection<Player> players = List.copyOf(proxy.getAllPlayers());
        TagResolver animation = animations.current();

        Map<UUID, Component> names = new HashMap<>();
        Map<UUID, Integer> orders = new HashMap<>();
        for (Player target : players) {
            Rank rank = ranks.of(target.getUniqueId());
            names.put(target.getUniqueId(), messages.render(playerFormat, TagResolver.resolver(animation,
                    Placeholder.component("prefix", rank.prefix()),
                    Placeholder.component("suffix", rank.suffix()),
                    Placeholder.unparsed("name", target.getUsername()),
                    Placeholder.unparsed("group", rank.group()),
                    Placeholder.unparsed("server", serverName(target)))));
            orders.put(target.getUniqueId(), sortByRank ? rank.weight() : 0);
        }

        Set<UUID> online = names.keySet();
        for (Player viewer : players) {
            TabList list = viewer.getTabList();
            for (Player target : players) {
                upsert(list, target, names.get(target.getUniqueId()), orders.get(target.getUniqueId()));
            }
            // Joueurs partis du réseau : seulement ceux qu'on a ajoutés nous-mêmes
            for (TabListEntry entry : List.copyOf(list.getEntries())) {
                UUID id = entry.getProfile().getId();
                if (managed.contains(id) && !online.contains(id)) {
                    list.removeEntry(id);
                }
            }
            updateHeaderFooter(viewer, players, animation);
        }
        managed.retainAll(online);
        managed.addAll(online);
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
                Placeholder.unparsed("server", server),
                Placeholder.unparsed("server_online", String.valueOf(serverOnline)),
                Placeholder.unparsed("ping", String.valueOf(viewer.getPing())),
                Placeholder.unparsed("time", timeFormat.format(now)),
                Placeholder.unparsed("date", dateFormat.format(now)),
                Placeholder.component("servers", servers(viewer, players, animation)));
        viewer.sendPlayerListHeaderAndFooter(messages.get(viewer, "tab.header", tags), messages.get(viewer, "tab.footer", tags));
    }

    /** « survie 3 · ressources 1 » : chaque serveur avec ses joueurs, au format de tab.server-entry. */
    private Component servers(Player viewer, Collection<Player> players, TagResolver animation) {
        List<Component> entries = new ArrayList<>();
        for (RegisteredServer registered : proxy.getAllServers()) {
            String name = registered.getServerInfo().getName();
            long count = players.stream().filter(player -> serverName(player).equals(name)).count();
            entries.add(messages.get(viewer, "tab.server-entry", TagResolver.resolver(animation,
                    Placeholder.unparsed("name", name),
                    Placeholder.unparsed("count", String.valueOf(count)))));
        }
        return Component.join(JoinConfiguration.separator(messages.get(viewer, "tab.server-separator", TagResolver.empty())), entries);
    }

    private static String serverName(Player player) {
        return player.getCurrentServer().map(ServerConnection::getServerInfo).map(info -> info.getName()).orElse("?");
    }
}
