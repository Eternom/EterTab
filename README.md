# EterTab

Affichage du réseau : un projet, **deux jars**. Document développeur, à tenir à jour avec le code.

| Jar | Où | Rôle |
|---|---|---|
| `EterTab-Velocity` | proxy Velocity 4.2+ | **Liste Tab de tout le réseau** (joueurs de tous les serveurs, grade LuckPerms, tri par grade, nom affiché des serveurs) avec en-tête et pied animés ; **MOTD** de la liste des serveurs ; **maintenance** ; `/etertab reload` |
| `EterTab-Paper` | chaque serveur Paper | **Sidebar** animée (grade, solde, serveur, joueurs du réseau, ping, PlaceholderAPI), masquable par chaque joueur avec `/sidebar` ; **grade au-dessus de la tête** |

Compilation : `gradlew build` → `velocity/build/libs/EterTab-Velocity-<version>.jar` et `paper/build/libs/EterTab-Paper-<version>.jar`.
La version Velocity est aussi écrite dans `@Plugin` (`EterTabVelocity`) : à garder identique à `gradle.properties`.
`common/` : code partagé par les deux jars (grades LuckPerms, animations), compilé dans chacun ; chaque plateforme ne fait que
lire sa propre config.

## Côté Velocity

- **Indépendant d'EterLib** (qui est pour Paper) : config, langues et palette propres (`plugins/etertab/`), mêmes conventions
  (MiniMessage, `lang/<locale>.yml`, `en_us` par défaut, palette identique à `EterLib/config.yml`).
- Les serveurs Paper envoient leurs propres joueurs à la liste et la réinitialisent au changement de serveur : EterTab
  complète avec les joueurs des autres serveurs (profil et skin venant du proxy) et réécrit noms et ordre à chaque
  rafraîchissement (`update-interval`), plus 0,5 s après chaque connexion/changement de serveur.
- Seuls les joueurs ajoutés par EterTab sont retirés de la liste : les PNJ d'autres plugins ne sont pas touchés.
- **Étiquettes** des plugins Paper (canal `eter:tab`, via EterLib `getPlayerTags()`) : `<tag_nom>` dans `tab.player-format`
  (ex : le métier d'EterMarket, `<tag_job>`, affiché par défaut dans la sidebar plutôt qu'ici), vide si absente. Posées seulement par le serveur où est le
  joueur, gardées jusqu'à sa déconnexion (le lobby montre encore le métier).
- LuckPerms (version Velocity) facultatif : sans lui, pas de grade et pas de tri par grade.
- **Noms affichés des serveurs** : rien à configurer ici. Chaque serveur Paper envoie le sien (`server-display-name`
  d'EterLib, canal `eter:server`, à chaque arrivée d'un joueur) ; `ServerNames` les garde dans `servers.yml` pour les
  connaître dès le redémarrage du proxy. Seul un serveur peut donner son nom (un message venant d'un joueur est
  ignoré) ; un serveur qui n'a encore rien envoyé affiche son nom de `velocity.toml`.
- **MOTD** (config.yml > motd, pas dans lang/ : le jeu n'envoie pas sa langue au ping) : plusieurs messages au hasard ou
  à tour de rôle, maximum affiché, lignes au survol (texte + joueurs connectés), texte de version, icône PNG 64x64.
- **Maintenance** : `/etertab maintenance on|off|status` (`etertab.admin`) ; seuls `etertab.maintenance.bypass` peuvent
  entrer, les autres sont renvoyés ; MOTD et texte de version dédiés ; état gardé dans `maintenance.yml`.
- `/etertab reload` relit config et langues sans redémarrer (sauf `update-interval`).

## Côté Paper

- Dépend d'**EterLib 1.7.0+** (étiquettes `<tag_…>`, textes communs, `Money` pour le solde, sidebar temporaire des autres plugins, langues, palette, préfixe commun des messages, `countOnline()` pour `<network>`, nom affiché du serveur
  `server-display-name` pour `<server>`, commun à tous les plugins).
- Un tableau de scores par joueur : sa sidebar (sans numéros rouges, seules les lignes modifiées sont renvoyées) et une
  équipe par joueur recopiée chez tous (grade au-dessus de la tête). Un autre plugin qui change le tableau d'un joueur
  remplace celui d'EterTab.
- `/sidebar` (`/sb`, `etertab.sidebar.toggle`, accordée à tous) : masque ou réaffiche sa sidebar ; choix gardé en base
  (`etertab_preferences`), donc sur tous les serveurs ; sidebar cachée le temps de lire ce choix (pas de clignotement).
- **Étiquettes** des autres plugins dans la sidebar : `<tag_nom>` (EterLib `getPlayerTags()`, ex : `<tag_job>` = métier
  d'EterMarket, sous l'argent) ; une ligne faite d'une étiquette absente disparaît.
- Un autre plugin peut **remplacer la sidebar un temps** via EterLib (`getSidebars()`, ex : la quête suivie dans
  EterMarket) : seul EterTab touche au tableau de scores. Ce remplacement, choisi par le joueur, passe avant `/sidebar`.
- Équipes nommées par poids de grade : sans proxy, le Tab de Minecraft est lui aussi trié par grade (`tab-order`).
- Collisions entre joueurs (règle de collision de leur équipe) : coupées d'office sur un lobby (EterHub installé),
  normales ailleurs ; `nametags.player-collisions` force l'un ou l'autre. Pas l'option de Paper, dont les équipes entreraient en conflit avec celles d'EterTab.
- Facultatifs : LuckPerms (grades), Vault + EterEconomy (`<balance>`), PlaceholderAPI (`%variables%`). Soldes et total
  du réseau sont relus en tâche de fond toutes les 5 s, jamais pendant l'affichage.
