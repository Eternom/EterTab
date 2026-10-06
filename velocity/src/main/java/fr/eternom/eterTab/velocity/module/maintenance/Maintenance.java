package fr.eternom.eterTab.velocity.module.maintenance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mode maintenance du réseau : MOTD dédié, et seuls les joueurs avec etertab.maintenance.bypass peuvent se connecter.
 * L'état est gardé dans plugins/etertab/maintenance.yml : il survit aux redémarrages du proxy.
 */
public class Maintenance {

    public static final String BYPASS = "etertab.maintenance.bypass";

    private final Path file;
    private volatile boolean enabled;

    public Maintenance(Path dataDirectory) throws IOException {
        this.file = dataDirectory.resolve("maintenance.yml");
        if (Files.exists(file)) {
            enabled = Files.readString(file, StandardCharsets.UTF_8).contains("enabled: true");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void set(boolean enabled) throws IOException {
        this.enabled = enabled;
        Files.writeString(file, "# Géré par /etertab maintenance on|off\nenabled: " + enabled + "\n", StandardCharsets.UTF_8);
    }
}
