package fr.eternom.eterTab.paper.module.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Un tableau de scores par joueur : sa sidebar lui est propre, et les équipes (noms au-dessus des têtes) y sont
 * recopiées pour tous les joueurs. Attention : un autre plugin qui change le tableau d'un joueur remplace celui-ci.
 */
public class Boards {

    private final Map<UUID, Scoreboard> boards = new ConcurrentHashMap<>();

    public Scoreboard create(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        return board;
    }

    public Scoreboard of(UUID player) {
        return boards.get(player);
    }

    public Collection<Scoreboard> all() {
        return boards.values();
    }

    public void remove(UUID player) {
        boards.remove(player);
    }
}
