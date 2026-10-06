package fr.eternom.eterTab.paper.module.scoreboard;

import fr.eternom.eterTab.paper.module.nametag.NametagService;
import fr.eternom.eterTab.paper.module.placeholder.Animations;
import fr.eternom.eterTab.paper.module.placeholder.Ranks;
import fr.eternom.eterTab.paper.module.placeholder.Ranks.Rank;
import fr.eternom.eterTab.paper.module.sidebar.SidebarService;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Rafraîchissement régulier (thread principal) : grade de chaque joueur, nom au-dessus de la tête, sidebar.
 * sidebar ou nametags valent null si désactivés dans la config.
 */
public class DisplayTask implements Runnable {

    private final Ranks ranks;
    private final Animations animations;
    private final SidebarService sidebar;
    private final NametagService nametags;

    public DisplayTask(Ranks ranks, Animations animations, SidebarService sidebar, NametagService nametags) {
        this.ranks = ranks;
        this.animations = animations;
        this.sidebar = sidebar;
        this.nametags = nametags;
    }

    @Override
    public void run() {
        TagResolver animation = animations.current();
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player, animation);
        }
    }

    public void update(Player player) {
        update(player, animations.current());
    }

    private void update(Player player, TagResolver animation) {
        Rank rank = ranks.of(player.getUniqueId());
        if (nametags != null) {
            nametags.update(player, rank);
        }
        if (sidebar != null) {
            sidebar.render(player, rank, animation);
        }
    }

    public NametagService nametags() {
        return nametags;
    }

    public SidebarService sidebar() {
        return sidebar;
    }
}
