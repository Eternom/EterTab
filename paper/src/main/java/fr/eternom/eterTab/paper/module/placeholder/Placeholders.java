package fr.eternom.eterTab.paper.module.placeholder;

import fr.eternom.eterLib.EterLib;
import fr.eternom.eterLib.helper.economy.Money;
import fr.eternom.eterLib.module.tag.PlayerTags;
import fr.eternom.eterLib.module.player.PlayerDirectory;
import fr.eternom.eterLib.module.rank.Ranks.Rank;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Variables des textes de la sidebar :
 * <player> <rank> <suffix> <group> <balance> <server> <online> <max> <network> <ping> <world> <x> <y> <z> <time> <date>,
 * les animations <anim_...>, et les %variables% de PlaceholderAPI s'il est installé.
 *
 * Le solde (Vault, donc EterEconomy) et le total du réseau (base) sont lus en tâche de fond toutes les quelques
 * secondes ({@link #refreshSlow}), jamais pendant l'affichage.
 */
public class Placeholders {

    private final boolean placeholderApi;
    private final PlayerDirectory players;
    private final String serverName;
    private final ZoneId zone;
    private final DateTimeFormatter timeFormat;
    private final DateTimeFormatter dateFormat;

    private final Map<UUID, String> balances = new ConcurrentHashMap<>();
    private volatile int networkOnline;

    public Placeholders(boolean placeholderApi, PlayerDirectory players, String serverName,
                        ZoneId zone, DateTimeFormatter timeFormat, DateTimeFormatter dateFormat) {
        this.placeholderApi = placeholderApi;
        this.players = players;
        this.serverName = serverName;
        this.zone = zone;
        this.timeFormat = timeFormat;
        this.dateFormat = dateFormat;
    }

    /** Asynchrone : soldes des joueurs connectés et total du réseau. */
    public void refreshSlow(Collection<UUID> online) {
        networkOnline = players.countOnline();
        balances.keySet().retainAll(online);
        Economy economy = Money.economy();
        if (economy != null) {
            for (UUID uuid : online) {
                balances.put(uuid, economy.format(economy.getBalance(Bukkit.getOfflinePlayer(uuid))));
            }
        }
    }


    public TagResolver tags(Player player, Rank rank, TagResolver animation) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        Location location = player.getLocation();
        return TagResolver.resolver(animation,
                Placeholder.unparsed("player", player.getName()),
                Placeholder.component("rank", rank.prefix()),
                Placeholder.component("suffix", rank.suffix()),
                Placeholder.unparsed("group", rank.group()),
                Placeholder.unparsed("balance", balances.getOrDefault(player.getUniqueId(), "-")),
                Placeholder.unparsed("server", serverName),
                Placeholder.unparsed("online", String.valueOf(Bukkit.getOnlinePlayers().size())),
                Placeholder.unparsed("max", String.valueOf(Bukkit.getMaxPlayers())),
                // Au moins les joueurs d'ici, le temps que le premier comptage du réseau arrive
                Placeholder.unparsed("network", String.valueOf(Math.max(networkOnline, Bukkit.getOnlinePlayers().size()))),
                Placeholder.unparsed("ping", String.valueOf(player.getPing())),
                Placeholder.unparsed("world", player.getWorld().getName()),
                Placeholder.unparsed("x", String.valueOf(location.getBlockX())),
                Placeholder.unparsed("y", String.valueOf(location.getBlockY())),
                Placeholder.unparsed("z", String.valueOf(location.getBlockZ())),
                Placeholder.unparsed("time", timeFormat.format(now)),
                Placeholder.unparsed("date", dateFormat.format(now)),
                playerTags(player));
    }

    /**
     * <tag_nom> : les étiquettes posées par les autres plugins pour ce joueur (EterLib, ex : <tag_job> = son métier
     * dans EterMarket) ; vide si absente.
     */
    private static TagResolver playerTags(Player player) {
        PlayerTags tags = EterLib.get().getPlayerTags();
        return new TagResolver() {
            @Override
            public Tag resolve(String name, ArgumentQueue arguments, Context context) {
                return has(name) ? Tag.selfClosingInserting(context.deserialize(tags.get(player, name.substring(4)))) : null;
            }

            @Override
            public boolean has(String name) {
                return name.startsWith("tag_");
            }
        };
    }

    /** Remplace les %variables% de PlaceholderAPI dans un texte brut (avant la mise en forme MiniMessage). */
    public String applyPlaceholderApi(Player player, String raw) {
        return placeholderApi ? PlaceholderAPI.setPlaceholders(player, raw) : raw;
    }
}
