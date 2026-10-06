package fr.eternom.eterTab.paper.module.placeholder;

import fr.eternom.eterLib.module.player.PlayerDirectory;
import fr.eternom.eterTab.common.Ranks.Rank;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

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

    private final boolean vault;
    private final boolean placeholderApi;
    private final PlayerDirectory players;
    private final String serverName;
    private final ZoneId zone;
    private final DateTimeFormatter timeFormat;
    private final DateTimeFormatter dateFormat;

    private final Map<UUID, String> balances = new ConcurrentHashMap<>();
    private volatile int networkOnline;

    public Placeholders(boolean vault, boolean placeholderApi, PlayerDirectory players, String serverName,
                        ZoneId zone, DateTimeFormatter timeFormat, DateTimeFormatter dateFormat) {
        this.vault = vault;
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
        Economy economy = economy();
        if (economy != null) {
            for (UUID uuid : online) {
                balances.put(uuid, economy.format(economy.getBalance(Bukkit.getOfflinePlayer(uuid))));
            }
        }
    }

    /**
     * Cherchée à chaque fois, pas une seule fois au démarrage : EterEconomy peut enregistrer son économie auprès
     * de Vault après le démarrage d'EterTab.
     */
    private Economy economy() {
        if (!vault) {
            return null;
        }
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        return provider == null ? null : provider.getProvider();
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
                Placeholder.unparsed("date", dateFormat.format(now)));
    }

    /** Remplace les %variables% de PlaceholderAPI dans un texte brut (avant la mise en forme MiniMessage). */
    public String applyPlaceholderApi(Player player, String raw) {
        return placeholderApi ? PlaceholderAPI.setPlaceholders(player, raw) : raw;
    }
}
