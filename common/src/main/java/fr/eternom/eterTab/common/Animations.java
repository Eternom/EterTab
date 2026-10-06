package fr.eternom.eterTab.common;

import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Animations de la config (animations.<nom>.interval en ms, animations.<nom>.frames), utilisables avec <anim_<nom>>.
 * L'image affichée dépend de l'heure : tous les joueurs voient la même au même moment.
 * Chaque plateforme (Paper, Velocity) lit sa config et fournit la liste.
 */
public class Animations {

    public record Animation(String name, long interval, List<String> frames) {
    }

    private final List<Animation> animations;

    public Animations(List<Animation> animations) {
        this.animations = animations.stream()
                .filter(animation -> !animation.frames().isEmpty())
                .map(animation -> new Animation(animation.name(), Math.max(50, animation.interval()), animation.frames()))
                .toList();
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
