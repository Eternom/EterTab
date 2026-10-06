package fr.eternom.eterTab.velocity.helper;

import com.velocitypowered.api.proxy.Player;
import fr.eternom.eterTab.velocity.core.Config;
import fr.eternom.eterTab.velocity.core.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MiniMessage avec la palette du réseau (<primary>, <accent>, <info>, <error>, <success>), comme EterLib côté Paper.
 * Les couleurs sont dans config.yml (colors), à garder identiques à EterLib/config.yml.
 */
public class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Lang lang;
    private final TagResolver palette;

    public Messages(Lang lang, Config config) {
        this.lang = lang;
        List<TagResolver> colors = new ArrayList<>();
        for (String name : config.getKeys("colors")) {
            TextColor color = parseColor(config.getString("colors." + name, "white"));
            if (color != null) {
                colors.add(TagResolver.resolver(name, Tag.styling(color)));
            }
        }
        this.palette = TagResolver.resolver(colors);
    }

    /** Texte de key dans la langue du joueur, mis en forme ; tags = variables et animations. */
    public Component get(Player player, String key, TagResolver tags) {
        String raw = lang.get(player.getEffectiveLocale().toString().toLowerCase(Locale.ROOT), key);
        return raw == null ? Component.text(key, NamedTextColor.RED) : render(raw, tags);
    }

    /** Texte de config (formats) mis en forme avec la palette. */
    public Component render(String raw, TagResolver tags) {
        return MINI_MESSAGE.deserialize(raw, palette, tags);
    }

    private static TextColor parseColor(String value) {
        return value.startsWith("#") ? TextColor.fromHexString(value) : NamedTextColor.NAMES.value(value.toLowerCase(Locale.ROOT));
    }
}
