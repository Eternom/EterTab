package fr.eternom.eterTab.paper;

import fr.eternom.eterLib.EterLib;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.helper.task.Tasks;
import fr.eternom.eterTab.paper.listeners.Commands;
import fr.eternom.eterTab.paper.listeners.Events;
import fr.eternom.eterTab.paper.module.nametag.NametagService;
import fr.eternom.eterTab.common.Animations;
import fr.eternom.eterTab.paper.module.placeholder.Placeholders;
import fr.eternom.eterTab.common.Ranks;
import fr.eternom.eterTab.paper.module.scoreboard.Boards;
import fr.eternom.eterTab.paper.module.scoreboard.DisplayTask;
import fr.eternom.eterTab.paper.module.sidebar.SidebarPreferences;
import fr.eternom.eterTab.paper.module.sidebar.SidebarService;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * EterTab côté Paper : sidebar et grade au-dessus de la tête. La liste Tab du réseau est gérée par
 * EterTab-Velocity ; sans proxy, le Tab de ce serveur est quand même trié par grade.
 */
public final class Main extends JavaPlugin {

    /** Version minimale d'EterLib : étiquettes du joueur (getPlayerTags) depuis 1.7.0. */
    private static final String REQUIRED_ETERLIB = "1.7.0";

    /** Soldes et total du réseau : relus en tâche de fond toutes les 5 secondes. */
    private static final long SLOW_REFRESH_TICKS = 5 * 20;
    /** Préfixe des tables d'EterTab dans la base commune : etertab_preferences. */
    private static final String TABLE_PREFIX = "etertab_";

    private Boards boards;
    private DisplayTask display;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // En premier : vérifie la version d'EterLib (un EterLib < 1.3.0 n'a pas requireVersion, d'où le catch)
        try {
            if (!EterLib.requireVersion(this, REQUIRED_ETERLIB)) {
                return;
            }
        } catch (LinkageError tooOld) {
            getLogger().severe("EterLib " + REQUIRED_ETERLIB + " ou plus récent est nécessaire.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        EterLib lib = EterLib.get();
        Messages messages = lib.messages(this, "en_us", "fr_fr");

        Ranks ranks = new Ranks(isEnabled("LuckPerms") ? LuckPermsProvider.get() : null);
        // Nom affiché du serveur (<server>) : celui d'EterLib, le même pour tous les plugins
        Placeholders placeholders = new Placeholders(isEnabled("PlaceholderAPI"), lib.getPlayers(),
                lib.getServerDisplayName(), ZoneId.of(getConfig().getString("time-zone", "Europe/Paris")),
                DateTimeFormatter.ofPattern(getConfig().getString("time-format", "HH:mm")),
                DateTimeFormatter.ofPattern(getConfig().getString("date-format", "dd/MM/yyyy")));

        boards = new Boards();
        boolean sidebarEnabled = getConfig().getBoolean("sidebar.enabled", true);
        // Préférences (sidebar masquée par /sidebar) : table etertab_preferences, partagée entre les serveurs
        SidebarPreferences preferences = sidebarEnabled ? new SidebarPreferences(lib.database(TABLE_PREFIX)) : null;
        SidebarService sidebar = sidebarEnabled
                ? new SidebarService(messages, placeholders, boards, preferences, new HashSet<>(getConfig().getStringList("sidebar.disabled-worlds")))
                : null;
        NametagService nametags = getConfig().getBoolean("nametags.enabled", true)
                ? new NametagService(boards, nameColor(), getConfig().getBoolean("tab-order", true),
                getConfig().getBoolean("nametags.player-collisions", !isLobby()))
                : null;
        display = new DisplayTask(ranks, animations(getConfig().getConfigurationSection("animations")), sidebar, nametags, messages);

        new Events(this, boards, display, preferences);
        new Commands(this, messages, preferences, sidebar);
        // Rechargement à chaud (/reload, PlugMan) : les joueurs déjà connectés ont aussi droit à leur tableau
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (preferences != null) {
                Tasks.async(this, () -> preferences.load(player.getUniqueId()), "Préférence de sidebar illisible pour " + player.getName());
            }
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

    /** Animations de config.yml (animations.<nom>.interval et .frames). */
    private static Animations animations(ConfigurationSection section) {
        List<Animations.Animation> animations = new ArrayList<>();
        if (section != null) {
            for (String name : section.getKeys(false)) {
                animations.add(new Animations.Animation(name, section.getLong(name + ".interval", 200),
                        section.getStringList(name + ".frames")));
            }
        }
        return new Animations(animations);
    }

    /** Serveur lobby : EterHub y est installé (chargé, même s'il démarre après EterTab). */
    private static boolean isLobby() {
        return Bukkit.getPluginManager().getPlugin("EterHub") != null;
    }

    private boolean isEnabled(String plugin) {
        boolean enabled = Bukkit.getPluginManager().isPluginEnabled(plugin);
        if (!enabled) {
            getLogger().info(plugin + " absent : ses informations ne seront pas affichées");
        }
        return enabled;
    }

    private NamedTextColor nameColor() {
        NamedTextColor color = NamedTextColor.NAMES.value(getConfig().getString("nametags.name-color", "white").toLowerCase(Locale.ROOT));
        return color == null ? NamedTextColor.WHITE : color;
    }
}
