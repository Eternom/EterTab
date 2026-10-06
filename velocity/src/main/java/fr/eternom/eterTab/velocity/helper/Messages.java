package fr.eternom.eterTab.velocity.helper;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import fr.eternom.eterTab.velocity.core.Config;
import fr.eternom.eterTab.velocity.core.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MiniMessage avec la palette du réseau (<primary>, <accent>, <info>, <error>, <success>), comme EterLib côté Paper.
 * Les couleurs sont dans config.yml (colors), à garder identiques à EterLib/config.yml.
 */
public class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    /** Codes § avec les couleurs exactes (§x§F§F§A§6§4§0), compris par les jeux récents : l'orange reste orange. */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

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

    /** Texte de key dans la langue du destinataire (joueur ; console = langue par défaut), mis en forme. */
    public Component get(CommandSource source, String key, TagResolver tags) {
        String locale = source instanceof Player player
                ? player.getEffectiveLocale().toString().toLowerCase(Locale.ROOT)
                : lang.getDefaultLocale();
        String raw = lang.get(locale, key);
        return raw == null ? Component.text(key, NamedTextColor.RED) : render(raw, tags);
    }

    /** Message de chat, précédé du préfixe (clé "prefix"). */
    public void send(CommandSource source, String key, TagResolver tags) {
        source.sendMessage(get(source, "prefix", TagResolver.empty()).append(get(source, key, tags)));
    }

    /** Texte de config (formats, MOTD) mis en forme avec la palette. */
    public Component render(String raw, TagResolver tags) {
        return MINI_MESSAGE.deserialize(raw, palette, tags);
    }

    /**
     * Version « codes § » d'un texte : pour les endroits où Minecraft n'accepte qu'un texte simple
     * (lignes au survol du nombre de joueurs, texte de version du MOTD).
     */
    public String legacy(String raw, TagResolver tags) {
        return LEGACY.serialize(render(raw, tags));
    }

    private static TextColor parseColor(String value) {
        return value.startsWith("#") ? TextColor.fromHexString(value) : NamedTextColor.NAMES.value(value.toLowerCase(Locale.ROOT));
    }
}
