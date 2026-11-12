package fr.eternom.eterTab.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Le réseau s'appelle EterCraft : l'ancien nom affiché (« ETERNOM », « E T E R N O M », « <bold>Eternom</bold> ») est
 * remplacé une fois dans les fichiers d'EterTab déjà installés (config.yml et lang/), qui ne reprennent jamais seuls
 * les nouveaux textes par défaut. Les adresses (eternom.fr, discord.gg/eternom) ne sont pas touchées.
 */
public final class Rebrand {

    private static final String[][] REPLACEMENTS = {
            {"E T E R N O M", "E T E R C R A F T"},
            {"ETERNOM", "ETERCRAFT"},
            {"<bold>Eternom</bold>", "<bold>EterCraft</bold>"},
    };

    private Rebrand() {
    }

    /** @return les fichiers modifiés (pour le dire dans la console) */
    public static List<Path> migrate(Path dataFolder) {
        List<Path> changed = new ArrayList<>();
        List<Path> files = new ArrayList<>();
        files.add(dataFolder.resolve("config.yml"));
        Path lang = dataFolder.resolve("lang");
        if (Files.isDirectory(lang)) {
            try (Stream<Path> list = Files.list(lang)) {
                list.filter(file -> file.toString().endsWith(".yml")).forEach(files::add);
            } catch (IOException ignored) {
                // dossier illisible : seulement config.yml
            }
        }
        for (Path file : files) {
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                String updated = text;
                for (String[] pair : REPLACEMENTS) {
                    updated = updated.replace(pair[0], pair[1]);
                }
                if (!updated.equals(text)) {
                    Files.writeString(file, updated, StandardCharsets.UTF_8);
                    changed.add(file);
                }
            } catch (IOException ignored) {
                // fichier illisible : laissé tel quel
            }
        }
        return changed;
    }
}
