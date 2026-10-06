package fr.eternom.eterTab.velocity.module.server;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nom affiché de chaque serveur, tel que le serveur l'a donné lui-même (server-display-name d'EterLib, envoyé à
 * chaque arrivée d'un joueur). Rien à configurer ici : les noms sont gardés dans plugins/etertab/servers.yml pour
 * être connus dès le redémarrage du proxy. Un serveur qui n'a encore rien envoyé garde son nom de velocity.toml.
 */
public class ServerNames {

    private final Path file;
    private final Logger logger;
    private final Map<String, String> names = new ConcurrentHashMap<>();

    public ServerNames(Path dataDirectory, Logger logger) throws IOException {
        this.file = dataDirectory.resolve("servers.yml");
        this.logger = logger;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                if (new Yaml().load(reader) instanceof Map<?, ?> map) {
                    map.forEach((server, name) -> names.put(String.valueOf(server), String.valueOf(name)));
                }
            }
        }
    }

    public String displayName(String server) {
        return names.getOrDefault(server, server);
    }

    /** Nom reçu d'un serveur ; le fichier n'est réécrit que s'il a changé. */
    public void update(String server, String displayName) {
        if (displayName.isBlank() || displayName.equals(names.put(server, displayName))) {
            return;
        }
        logger.info("Nom affiché du serveur {} : {}", server, displayName);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "# Géré automatiquement : chaque serveur envoie son nom (server-display-name d'EterLib)\n"
                    + new Yaml().dump(new TreeMap<>(names)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("servers.yml non enregistré", e);
        }
    }
}
