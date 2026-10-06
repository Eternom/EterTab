package fr.eternom.eterTab.paper.module.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;

public class BoardListener implements Listener {

    private final Boards boards;
    private final DisplayTask display;

    public BoardListener(Boards boards, DisplayTask display) {
        this.boards = boards;
        this.display = display;
    }

    /** Tableau du joueur, avec les équipes de ceux déjà là ; puis son équipe recopiée chez les autres. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
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
        boards.remove(player.getUniqueId());
    }
}
