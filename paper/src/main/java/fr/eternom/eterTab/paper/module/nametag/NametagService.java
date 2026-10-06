package fr.eternom.eterTab.paper.module.nametag;

import fr.eternom.eterTab.paper.module.placeholder.Ranks.Rank;
import fr.eternom.eterTab.paper.module.scoreboard.Boards;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grade au-dessus de la tête (préfixe et suffixe LuckPerms), via une équipe par joueur, recopiée dans le tableau
 * de chaque joueur connecté. Le nom de l'équipe commence par le poids du grade : sans proxy, le Tab de Minecraft
 * (trié par équipe) est donc aussi trié par grade.
 */
public class NametagService {

    private record Applied(String team, Rank rank) {
    }

    private final Boards boards;
    private final NamedTextColor nameColor;
    private final boolean tabOrder;
    private final Map<UUID, Applied> applied = new ConcurrentHashMap<>();

    public NametagService(Boards boards, NamedTextColor nameColor, boolean tabOrder) {
        this.boards = boards;
        this.nameColor = nameColor;
        this.tabOrder = tabOrder;
    }

    /** Thread principal, à chaque rafraîchissement : ne fait quelque chose que si le grade a changé. */
    public void update(Player player, Rank rank) {
        Applied previous = applied.get(player.getUniqueId());
        if (previous != null && previous.rank().equals(rank)) {
            return;
        }
        String team = teamName(player, rank);
        for (Scoreboard board : boards.all()) {
            if (previous != null && !previous.team().equals(team)) {
                unregister(board, previous.team()); // le poids a changé : l'équipe change de nom (donc de place)
            }
            write(board, team, player.getName(), rank);
        }
        applied.put(player.getUniqueId(), new Applied(team, rank));
        if (tabOrder) {
            player.setPlayerListOrder(rank.weight());
        }
    }

    /** Nouveau tableau (joueur qui arrive) : y recopie les équipes de tous les joueurs déjà là. */
    public void copyAllTo(Scoreboard board, Iterable<? extends Player> online) {
        for (Player player : online) {
            Applied current = applied.get(player.getUniqueId());
            if (current != null) {
                write(board, current.team(), player.getName(), current.rank());
            }
        }
    }

    public void remove(Player player) {
        Applied previous = applied.remove(player.getUniqueId());
        if (previous != null) {
            boards.all().forEach(board -> unregister(board, previous.team()));
        }
    }

    private void write(Scoreboard board, String teamName, String entry, Rank rank) {
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        team.prefix(rank.prefix());
        team.suffix(rank.suffix());
        team.color(nameColor);
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }
    }

    private static void unregister(Scoreboard board, String teamName) {
        Team team = board.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
    }

    /** « 99899-1a2b3c4d » : plus le poids est grand, plus le nom est petit, donc plus haut dans le Tab. */
    private static String teamName(Player player, Rank rank) {
        int order = 99999 - Math.clamp(rank.weight(), 0, 99999);
        return String.format("%05d-%s", order, player.getUniqueId().toString().substring(0, 8));
    }
}
