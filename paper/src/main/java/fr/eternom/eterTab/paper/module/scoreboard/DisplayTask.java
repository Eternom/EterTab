package fr.eternom.eterTab.paper.module.scoreboard;

import fr.eternom.eterLib.EterLib;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterTab.common.Animations;
import fr.eternom.eterTab.common.Ranks;
import fr.eternom.eterTab.common.Ranks.Rank;
import fr.eternom.eterTab.paper.module.nametag.NametagService;
import fr.eternom.eterTab.paper.module.sidebar.SidebarService;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Rafraîchissement régulier (thread principal) : grade de chaque joueur, nom au-dessus de la tête, sidebar.
 * sidebar ou nametags valent null si désactivés dans la config.
 */
public class DisplayTask implements Runnable {

    /** Étiquette EterLib qui remplace le grade (posée par EterClan : le tag du clan). */
    private static final String BADGE = "badge";

    private final Ranks ranks;
    private final Animations animations;
    private final SidebarService sidebar;
    private final NametagService nametags;
    private final Messages messages;

    public DisplayTask(Ranks ranks, Animations animations, SidebarService sidebar, NametagService nametags, Messages messages) {
        this.ranks = ranks;
        this.messages = messages;
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
        // Un badge (tag de clan...) remplace le grade : au-dessus de la tête et dans la sidebar (<rank>)
        String badge = EterLib.get().getPlayerTags().get(player, BADGE);
        Rank rank = ranks.of(player.getUniqueId())
                .withBadge(badge == null || badge.isBlank() ? null : messages.render(badge, TagResolver.empty()));
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
