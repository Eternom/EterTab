package fr.eternom.eterTab.paper.module.sidebar;

import fr.eternom.eterLib.EterLib;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.helper.sidebar.SidebarOverrides;
import fr.eternom.eterTab.paper.module.placeholder.Placeholders;
import fr.eternom.eterLib.module.rank.Ranks.Rank;
import fr.eternom.eterTab.paper.module.scoreboard.Boards;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sidebar (tableau à droite de l'écran), dans la langue du joueur : titre sidebar.title et lignes sidebar.lines
 * (une liste dans lang/). Sans numéros rouges ; seules les lignes qui changent sont renvoyées au joueur.
 * Un autre plugin peut la remplacer un temps (EterLib getSidebars, ex : la quête suivie dans EterMarket).
 */
public class SidebarService {

    private static final String OBJECTIVE = "eter_sidebar";
    /** Limite de Minecraft. */
    private static final int MAX_LINES = 15;

    private final Messages messages;
    private final Placeholders placeholders;
    private final Boards boards;
    private final SidebarPreferences preferences;
    private final Set<String> disabledWorlds;

    private final Map<UUID, Component> titles = new ConcurrentHashMap<>();
    private final Map<UUID, List<Component>> shown = new ConcurrentHashMap<>();

    public SidebarService(Messages messages, Placeholders placeholders, Boards boards, SidebarPreferences preferences,
                          Set<String> disabledWorlds) {
        this.messages = messages;
        this.placeholders = placeholders;
        this.boards = boards;
        this.preferences = preferences;
        this.disabledWorlds = disabledWorlds;
    }

    /** Thread principal. */
    public void render(Player player, Rank rank, TagResolver animation) {
        UUID uuid = player.getUniqueId();
        Scoreboard board = boards.of(uuid);
        if (board == null) {
            return;
        }
        Objective objective = board.getObjective(OBJECTIVE);
        // Sidebar temporaire d'un autre plugin (ex : quête suivie) : choisie par le joueur, elle passe avant /sidebar
        SidebarOverrides.Content override = EterLib.get().getSidebars().content(player);
        if ((override == null && preferences.isHidden(uuid)) || disabledWorlds.contains(player.getWorld().getName())) {
            hide(player);
            return;
        }

        Component title;
        List<Component> lines;
        if (override != null) {
            title = override.title();
            lines = override.lines().size() > MAX_LINES ? override.lines().subList(0, MAX_LINES) : override.lines();
        } else {
            TagResolver tags = placeholders.tags(player, rank, animation);
            title = render(player, messages.raw(player, "sidebar.title"), tags);
            lines = lines(player, tags);
        }

        if (objective == null) {
            objective = board.registerNewObjective(OBJECTIVE, Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.numberFormat(NumberFormat.blank()); // pas de numéros rouges à droite
            forget(uuid);
        }
        if (!title.equals(titles.get(uuid))) {
            objective.displayName(title);
            titles.put(uuid, title);
        }

        List<Component> previous = shown.getOrDefault(uuid, List.of());
        for (int i = 0; i < lines.size(); i++) {
            // Une entrée invisible par ligne ; le score fixe l'ordre (la première ligne a le plus grand)
            Score score = objective.getScore(entry(i));
            if (i >= previous.size()) {
                score.setScore(MAX_LINES - i);
            }
            if (i >= previous.size() || !lines.get(i).equals(previous.get(i))) {
                score.customName(lines.get(i));
            }
        }
        for (int i = lines.size(); i < previous.size(); i++) {
            board.resetScores(entry(i));
        }
        shown.put(uuid, List.copyOf(lines));
    }

    /** Retire la sidebar du joueur (masquée par /sidebar, ou monde où elle est désactivée). */
    public void hide(Player player) {
        Scoreboard board = boards.of(player.getUniqueId());
        Objective objective = board == null ? null : board.getObjective(OBJECTIVE);
        if (objective != null) {
            objective.unregister();
        }
        forget(player.getUniqueId());
    }

    public void forget(UUID player) {
        titles.remove(player);
        shown.remove(player);
    }

    private List<Component> lines(Player player, TagResolver tags) {
        String raw = messages.raw(player, "sidebar.lines");
        List<Component> lines = new ArrayList<>();
        if (raw == null) {
            return lines;
        }
        for (String line : raw.split("\n", -1)) {
            if (lines.size() == MAX_LINES) {
                break;
            }
            Component rendered = render(player, line, tags);
            // Une ligne faite d'une étiquette absente (ex : <tag_job> sur un serveur sans EterMarket) disparaît
            if (line.contains("<tag_") && PlainTextComponentSerializer.plainText().serialize(rendered).isBlank()) {
                continue;
            }
            lines.add(rendered);
        }
        return lines;
    }

    private Component render(Player player, String raw, TagResolver tags) {
        return raw == null ? Component.empty() : messages.render(placeholders.applyPlaceholderApi(player, raw), tags);
    }

    private static String entry(int line) {
        return "eter-line-" + line;
    }
}
