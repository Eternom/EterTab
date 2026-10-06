package fr.eternom.eterTab.paper;

import fr.eternom.eterLib.EterLib;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterTab.paper.listeners.Events;
import fr.eternom.eterTab.paper.module.nametag.NametagService;
import fr.eternom.eterTab.paper.module.placeholder.Animations;
import fr.eternom.eterTab.paper.module.placeholder.Placeholders;
import fr.eternom.eterTab.paper.module.placeholder.Ranks;
import fr.eternom.eterTab.paper.module.scoreboard.Boards;
import fr.eternom.eterTab.paper.module.scoreboard.DisplayTask;
import fr.eternom.eterTab.paper.module.sidebar.SidebarService;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPermsProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;

/**
 * EterTab côté Paper : sidebar et grade au-dessus de la tête. La liste Tab du réseau est gérée par
 * EterTab-Velocity ; sans proxy, le Tab de ce serveur est quand même trié par grade.
 */
public final class Main extends JavaPlugin {

    /** Version minimale d'EterLib : les méthodes utilisées par ce plugin n'existent pas avant. */
    private static final String REQUIRED_ETERLIB = "1.2.0";

    /** Soldes et total du réseau : relus en tâche de fond toutes les 5 secondes. */
    private static final long SLOW_REFRESH_TICKS = 5 * 20;

    private Boards boards;
    private DisplayTask display;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        EterLib lib = EterLib.get();
        if (!isAtLeast(lib.getPluginMeta().getVersion(), REQUIRED_ETERLIB)) {
            getLogger().severe("EterTab nécessite EterLib " + REQUIRED_ETERLIB + " ou plus récent (installé : "
                    + lib.getPluginMeta().getVersion() + "). Plugin désactivé.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Messages messages = lib.messages(this, "en_us", "fr_fr");

        Ranks ranks = new Ranks(isEnabled("LuckPerms") ? LuckPermsProvider.get() : null);
        Placeholders placeholders = new Placeholders(economy(), isEnabled("PlaceholderAPI"), lib.getPlayers(),
                lib.getServerName(), ZoneId.of(getConfig().getString("time-zone", "Europe/Paris")),
                DateTimeFormatter.ofPattern(getConfig().getString("time-format", "HH:mm")),
                DateTimeFormatter.ofPattern(getConfig().getString("date-format", "dd/MM/yyyy")));

        boards = new Boards();
        SidebarService sidebar = getConfig().getBoolean("sidebar.enabled", true)
                ? new SidebarService(messages, placeholders, boards, new HashSet<>(getConfig().getStringList("sidebar.disabled-worlds")))
                : null;
        NametagService nametags = getConfig().getBoolean("nametags.enabled", true)
                ? new NametagService(boards, nameColor(), getConfig().getBoolean("tab-order", true))
                : null;
        display = new DisplayTask(ranks, new Animations(getConfig().getConfigurationSection("animations")), sidebar, nametags);

        new Events(this, boards, display);
        // Rechargement à chaud (/reload, PlugMan) : les joueurs déjà connectés ont aussi droit à leur tableau
        Bukkit.getOnlinePlayers().forEach(player -> {
            Scoreboard board = boards.create(player);
            if (nametags != null) {
                nametags.copyAllTo(board, Bukkit.getOnlinePlayers());
            }
        });

        long interval = Math.max(1, getConfig().getLong("update-interval", 20));
        Bukkit.getScheduler().runTaskTimer(this, display, interval, interval);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> placeholders.refreshSlow(
                Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList()), 1, SLOW_REFRESH_TICKS);

        getLogger().info("Sidebar " + (sidebar != null ? "active" : "désactivée") + ", noms au-dessus des têtes "
                + (nametags != null ? "actifs" : "désactivés"));
    }

    @Override
    public void onDisable() {
        // Rend à chacun le tableau principal du serveur
        if (boards != null) {
            Bukkit.getOnlinePlayers().forEach(player -> player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()));
        }
    }

    private boolean isEnabled(String plugin) {
        boolean enabled = Bukkit.getPluginManager().isPluginEnabled(plugin);
        if (!enabled) {
            getLogger().info(plugin + " absent : ses informations ne seront pas affichées");
        }
        return enabled;
    }

    private Economy economy() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            getLogger().info("Vault absent : pas de solde dans la sidebar");
            return null;
        }
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        return provider == null ? null : provider.getProvider();
    }

    private NamedTextColor nameColor() {
        NamedTextColor color = NamedTextColor.NAMES.value(getConfig().getString("nametags.name-color", "white").toLowerCase(Locale.ROOT));
        return color == null ? NamedTextColor.WHITE : color;
    }

    /** "1.2.0" >= "1.1.2" : compare les nombres un à un (un suffixe comme -SNAPSHOT est ignoré). */
    private static boolean isAtLeast(String version, String minimum) {
        String[] actual = version.split("[.-]");
        String[] wanted = minimum.split("[.-]");
        for (int i = 0; i < wanted.length; i++) {
            int a = i < actual.length && actual[i].matches("\\d+") ? Integer.parseInt(actual[i]) : 0;
            int w = Integer.parseInt(wanted[i]);
            if (a != w) {
                return a > w;
            }
        }
        return true;
    }
}
