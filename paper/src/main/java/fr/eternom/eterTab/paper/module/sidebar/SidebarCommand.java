package fr.eternom.eterTab.paper.module.sidebar;

import fr.eternom.eterLib.helper.message.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Level;

/** /sidebar : masque ou réaffiche sa sidebar ; le choix est gardé pour tous les serveurs. */
public class SidebarCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final SidebarPreferences preferences;
    private final SidebarService sidebar;
    private final Messages messages;

    public SidebarCommand(JavaPlugin plugin, SidebarPreferences preferences, SidebarService sidebar, Messages messages) {
        this.plugin = plugin;
        this.preferences = preferences;
        this.sidebar = sidebar;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return true;
        }
        UUID uuid = player.getUniqueId();
        boolean hidden = preferences.toggle(uuid);
        if (hidden) {
            sidebar.hide(player);
        }
        // Réaffichage : la sidebar revient au prochain rafraîchissement (une seconde au plus)
        messages.send(player, hidden ? "sidebar.hidden" : "sidebar.shown");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                preferences.save(uuid);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "Préférence de sidebar non enregistrée pour " + player.getName(), e);
            }
        });
        return true;
    }
}
