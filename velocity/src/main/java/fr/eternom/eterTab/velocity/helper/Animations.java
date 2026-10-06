package fr.eternom.eterTab.velocity.helper;

import fr.eternom.eterTab.velocity.core.Config;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Animations de config.yml (animations.<nom>.interval en ms, animations.<nom>.frames) : utilisables partout
 * avec la balise <anim_<nom>>. L'image affichée dépend de l'heure : tous les joueurs voient la même au même moment.
 */
public class Animations {

    private record Animation(String name, long interval, List<String> frames) {
    }

    private final List<Animation> animations = new ArrayList<>();

    public Animations(Config config) {
        for (String name : config.getKeys("animations")) {
            List<String> frames = config.getStringList("animations." + name + ".frames");
            if (!frames.isEmpty()) {
                animations.add(new Animation(name, Math.max(50, config.getInt("animations." + name + ".interval", 200)), frames));
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
