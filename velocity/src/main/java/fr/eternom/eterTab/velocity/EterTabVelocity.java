package fr.eternom.eterTab.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.eternom.eterTab.velocity.core.Config;
import fr.eternom.eterTab.velocity.core.Lang;
import fr.eternom.eterTab.common.Animations;
import fr.eternom.eterTab.velocity.helper.Messages;
import fr.eternom.eterTab.velocity.listeners.Events;
import fr.eternom.eterTab.velocity.module.maintenance.Maintenance;
import fr.eternom.eterTab.velocity.module.motd.MotdService;
import fr.eternom.eterTab.velocity.module.server.ServerNames;
import fr.eternom.eterTab.common.Ranks;
import fr.eternom.eterTab.velocity.module.tab.TabService;
import fr.eternom.eterTab.velocity.module.tab.TabTags;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * EterTab côté Velocity : liste Tab de tout le réseau (joueurs de tous les serveurs, grades LuckPerms, tri par grade),
 * en-tête et pied animés dans la langue de chaque joueur, MOTD de la liste des serveurs et mode maintenance.
 * Indépendant d'EterLib (qui est pour Paper). /etertab reload relit config et langues sans redémarrer.
 */
@Plugin(id = "etertab", name = "EterTab", version = "1.1.11", authors = {"NadTum"},
        description = "Liste Tab du réseau, MOTD et maintenance",
        dependencies = {@Dependency(id = "luckperms", optional = true)})
public final class EterTabVelocity {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    // Remplacés à chaque /etertab reload ; volatile : lus par les tâches et les événements
    private volatile Config config;
    private volatile Messages messages;
    private volatile TabService tab;
    private volatile MotdService motd;
    private Maintenance maintenance;
    private ServerNames serverNames;
    private final TabTags tabTags = new TabTags();

    @Inject
    public EterTabVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        try {
            maintenance = new Maintenance(dataDirectory);
            serverNames = new ServerNames(dataDirectory, logger);
        } catch (IOException e) {
            logger.error("maintenance.yml ou servers.yml illisible, EterTab désactivé", e);
            return;
        }
        if (!load()) {
            logger.error("Configuration illisible, EterTab désactivé");
            return;
        }
        new Events(this);

        // L'intervalle n'est lu qu'au démarrage ; la tâche utilise toujours le TabService du moment
        Duration interval = Duration.ofMillis(Math.max(100, config.getInt("update-interval", 1000)));
        proxy.getScheduler().buildTask(this, () -> tab.refreshAll()).repeat(interval).schedule();
        logger.info("Liste Tab du réseau et MOTD actifs{}", maintenance.isEnabled() ? " (MAINTENANCE ACTIVÉE)" : "");
    }

    /** (Re)lit config et langues et reconstruit les services. @return false si la config est illisible (rien n'est changé). */
    public boolean load() {
        try {
            Config newConfig = new Config(dataDirectory);
            Messages newMessages = new Messages(new Lang(dataDirectory, newConfig.getString("default-language", "en_us"), logger), newConfig);
            Animations animations = animations(newConfig);

            boolean luckPerms = proxy.getPluginManager().isLoaded("luckperms");
            if (!luckPerms) {
                logger.warn("LuckPerms absent : pas de grades dans la liste Tab");
            }
            Ranks ranks = new Ranks(luckPerms ? LuckPermsProvider.get() : null);

            TabService newTab = new TabService(proxy, newMessages, animations, ranks, serverNames, tabTags, newConfig);
            MotdService newMotd = new MotdService(proxy, newMessages, animations, maintenance, newConfig, dataDirectory, logger);
            config = newConfig;
            messages = newMessages;
            tab = newTab;
            motd = newMotd;
            return true;
        } catch (IOException | RuntimeException e) {
            logger.error("Erreur en lisant la configuration d'EterTab", e);
            return false;
        }
    }

    /** Animations de config.yml (animations.<nom>.interval et .frames). */
    private static Animations animations(Config config) {
        List<Animations.Animation> animations = new ArrayList<>();
        for (String name : config.getKeys("animations")) {
            animations.add(new Animations.Animation(name, config.getInt("animations." + name + ".interval", 200),
                    config.getStringList("animations." + name + ".frames")));
        }
        return new Animations(animations);
    }

    public ProxyServer proxy() {
        return proxy;
    }

    public Messages messages() {
        return messages;
    }

    public TabService tab() {
        return tab;
    }

    public MotdService motd() {
        return motd;
    }

    public Maintenance maintenance() {
        return maintenance;
    }

    public ServerNames serverNames() {
        return serverNames;
    }

    public TabTags tabTags() {
        return tabTags;
    }
}
