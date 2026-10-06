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
import fr.eternom.eterTab.velocity.helper.Animations;
import fr.eternom.eterTab.velocity.helper.Messages;
import fr.eternom.eterTab.velocity.listeners.Events;
import fr.eternom.eterTab.velocity.module.tab.Ranks;
import fr.eternom.eterTab.velocity.module.tab.TabService;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

/**
 * EterTab côté Velocity : liste Tab de tout le réseau (joueurs de tous les serveurs, grades LuckPerms, tri par grade)
 * avec en-tête et pied animés dans la langue de chaque joueur. Indépendant d'EterLib (qui est pour Paper).
 */
@Plugin(id = "etertab", name = "EterTab", version = "1.0.1", authors = {"NadTum"},
        description = "Liste Tab de tout le réseau",
        dependencies = {@Dependency(id = "luckperms", optional = true)})
public final class EterTabVelocity {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public EterTabVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        Config config;
        Messages messages;
        try {
            config = new Config(dataDirectory);
            messages = new Messages(new Lang(dataDirectory, config.getString("default-language", "en_us"), logger), config);
        } catch (IOException e) {
            logger.error("Configuration illisible, EterTab désactivé", e);
            return;
        }

        boolean luckPerms = proxy.getPluginManager().isLoaded("luckperms");
        Ranks ranks = new Ranks(luckPerms ? LuckPermsProvider.get() : null);
        if (!luckPerms) {
            logger.warn("LuckPerms absent : pas de grades dans la liste Tab");
        }

        TabService tab = new TabService(proxy, messages, new Animations(config), ranks, config);
        new Events(this, proxy, tab);

        Duration interval = Duration.ofMillis(Math.max(100, config.getInt("update-interval", 1000)));
        proxy.getScheduler().buildTask(this, tab::refreshAll).repeat(interval).schedule();
        logger.info("Liste Tab du réseau active");
    }
}
