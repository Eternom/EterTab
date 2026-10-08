package fr.eternom.eterTab.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;

import java.util.UUID;

/**
 * Grade d'un joueur depuis LuckPerms (Paper ou Velocity : même API) : préfixe, suffixe, poids et nom du groupe principal.
 * Sans LuckPerms, tout le monde a un grade vide (le plugin fonctionne quand même).
 */
public class Ranks {

    /** record : deux grades identiques sont égaux, ce qui permet de ne redessiner qu'en cas de changement. */
    public record Rank(Component prefix, Component suffix, int weight, String group) {

        public static final Rank NONE = new Rank(Component.empty(), Component.empty(), 0, "default");

        /**
         * Badge posé par un plugin (étiquette « badge » : tag de clan...) : il remplace le préfixe du grade ; vide = le
         * grade. Le poids (ordre du Tab) ne change pas.
         */
        public Rank withBadge(Component badge) {
            return badge == null ? this : new Rank(badge, suffix, weight, group);
        }
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
        return new Rank(spaced(parse(meta.getPrefix()), true), spaced(parse(meta.getSuffix()), false),
                group == null ? 0 : group.getWeight().orElse(0), user.getPrimaryGroup());
    }

    /** Une espace entre le grade et le pseudo, si le préfixe (ou le suffixe) LuckPerms n'en a pas. */
    private static Component spaced(Component part, boolean prefix) {
        String plain = PlainTextComponentSerializer.plainText().serialize(part);
        if (plain.isBlank()) {
            return Component.empty();
        }
        if (prefix) {
            return plain.endsWith(" ") ? part : part.append(Component.space());
        }
        return plain.startsWith(" ") ? part : Component.space().append(part);
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
