package fr.eternom.eterTab.velocity.module.tab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;

import java.util.UUID;

/**
 * Grade d'un joueur depuis LuckPerms (version Velocity) : préfixe, suffixe, poids du groupe principal.
 * Sans LuckPerms installé, tout le monde a un grade vide et le même poids.
 */
public class Ranks {

    public record Rank(Component prefix, Component suffix, int weight, String group) {

        static final Rank NONE = new Rank(Component.empty(), Component.empty(), 0, "default");
    }

    private final LuckPerms luckPerms; // null si LuckPerms n'est pas installé

    public Ranks(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public Rank of(UUID player) {
        if (luckPerms == null) {
            return Rank.NONE;
        }
        User user = luckPerms.getUserManager().getUser(player);
        if (user == null) {
            return Rank.NONE;
        }
        CachedMetaData meta = user.getCachedData().getMetaData();
        Group group = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
        return new Rank(parse(meta.getPrefix()), parse(meta.getSuffix()),
                group == null ? 0 : group.getWeight().orElse(0), user.getPrimaryGroup());
    }

    /** Les préfixes LuckPerms sont souvent en codes « & » ; sinon on les lit comme du MiniMessage. */
    private static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (text.indexOf('&') >= 0 || text.indexOf('§') >= 0) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text.replace('§', '&'));
        }
        return MiniMessage.miniMessage().deserialize(text);
    }
}
