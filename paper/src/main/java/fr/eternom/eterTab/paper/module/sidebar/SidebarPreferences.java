package fr.eternom.eterTab.paper.module.sidebar;

import fr.eternom.eterLib.helper.sql.Column;
import fr.eternom.eterLib.helper.sql.Database;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Joueurs qui ont masqué leur sidebar (/sidebar). Enregistré en base (etertab_preferences) : le choix suit le joueur
 * sur tous les serveurs et après une déconnexion. Gardé en mémoire pour les joueurs connectés.
 */
public class SidebarPreferences {

    private static final String TABLE = "preferences";

    private final Database database;
    private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();
    /** Préférence pas encore lue (arrivée) : sidebar cachée en attendant, pour qu'elle ne clignote pas. */
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();

    public SidebarPreferences(Database database) {
        this.database = database;
        database.createTable(TABLE,
                Column.of("uuid", Column.Type.UUID).primaryKey(),
                Column.of("sidebar_hidden", Column.Type.BOOLEAN).notNull());
    }

    public boolean isHidden(UUID player) {
        return hidden.contains(player) || loading.contains(player);
    }

    /** Thread principal, à l'arrivée : cache la sidebar jusqu'à ce que {@link #load} ait lu la préférence. */
    public void startLoading(UUID player) {
        loading.add(player);
    }

    /** Bloquant (base) : à l'arrivée du joueur, hors du thread principal. */
    public void load(UUID player) {
        boolean isHidden = database.getFirst(TABLE, Map.of("uuid", player))
                .map(row -> row.getBoolean("sidebar_hidden"))
                .orElse(false);
        if (isHidden) {
            hidden.add(player);
        } else {
            hidden.remove(player);
        }
        loading.remove(player);
    }

    /** Thread principal : bascule l'affichage tout de suite ; l'écriture en base se fait ensuite (save, en async). */
    public boolean toggle(UUID player) {
        if (!hidden.remove(player)) {
            hidden.add(player);
        }
        return hidden.contains(player);
    }

    /** Bloquant (base). */
    public void save(UUID player) {
        database.set(TABLE, Map.of("uuid", player, "sidebar_hidden", hidden.contains(player)), "uuid");
    }

    public void forget(UUID player) {
        hidden.remove(player);
        loading.remove(player);
    }
}
