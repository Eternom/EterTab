package fr.eternom.eterTab.paper.module.placeholder;

import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Animations de config.yml (animations.<nom>.interval en ms, animations.<nom>.frames), utilisables avec <anim_<nom>>.
 * L'image affichée dépend de l'heure : tous les joueurs voient la même au même moment.
 */
public class Animations {

    private record Animation(String name, long interval, List<String> frames) {
    }

    private final List<Animation> animations = new ArrayList<>();

    public Animations(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String name : section.getKeys(false)) {
            List<String> frames = section.getStringList(name + ".frames");
            if (!frames.isEmpty()) {
                animations.add(new Animation(name, Math.max(50, section.getLong(name + ".interval", 200)), frames));
            }
        }
    }

    /** Balises <anim_...> avec l'image du moment (texte MiniMessage de la config, donc de confiance). */
    public TagResolver current() {
        long now = System.currentTimeMillis();
        List<TagResolver> resolvers = new ArrayList<>();
        for (Animation animation : animations) {
            String frame = animation.frames().get((int) ((now / animation.interval()) % animation.frames().size()));
            resolvers.add(TagResolver.resolver("anim_" + animation.name(), Tag.preProcessParsed(frame)));
        }
        return TagResolver.resolver(resolvers);
    }
}
