package fr.eternom.eterTab.paper.module.scoreboard;

import fr.eternom.eterLib.helper.task.Tasks;
import fr.eternom.eterTab.paper.module.sidebar.SidebarPreferences;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;

public class BoardListener implements Listener {

    private final JavaPlugin plugin;
    private final Boards boards;
    private final DisplayTask display;
    private final SidebarPreferences preferences; // null si la sidebar est désactivée

    public BoardListener(JavaPlugin plugin, Boards boards, DisplayTask display, SidebarPreferences preferences) {
        this.plugin = plugin;
        this.boards = boards;
        this.display = display;
        this.preferences = preferences;
    }

    /** Tableau du joueur, avec les équipes de ceux déjà là ; puis son équipe recopiée chez les autres. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (preferences != null) {
            preferences.startLoading(uuid);
            Tasks.async(plugin, () -> {
                try {
                    preferences.load(uuid);
                } catch (RuntimeException e) {
                    // Base injoignable : on affiche la sidebar par défaut plutôt que de la cacher pour toujours
                    preferences.forget(uuid);
                    throw e;
                }
            }, "Préférence de sidebar illisible pour " + player.getName());
        }
        Scoreboard board = boards.create(player);
        if (display.nametags() != null) {
            display.nametags().copyAllTo(board, Bukkit.getOnlinePlayers());
        }
        display.update(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (display.nametags() != null) {
            display.nametags().remove(player);
        }
        if (display.sidebar() != null) {
            display.sidebar().forget(player.getUniqueId());
        }
        if (preferences != null) {
            preferences.forget(player.getUniqueId());
        }
        boards.remove(player.getUniqueId());
    }
}
