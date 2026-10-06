# EterTab

Affichage du réseau : un projet, **deux jars**. Document développeur, à tenir à jour avec le code.

| Jar | Où | Rôle |
|---|---|---|
| `EterTab-Velocity` | proxy Velocity 4.2+ | **Liste Tab de tout le réseau** : joueurs de tous les serveurs, grade LuckPerms, tri par poids de grade, ping ; en-tête et pied animés dans la langue de chaque joueur (total réseau, joueurs par serveur, heure…) |
| `EterTab-Paper` | chaque serveur Paper | **Sidebar** animée (grade, solde, serveur, joueurs du réseau, ping, PlaceholderAPI) et **grade au-dessus de la tête** |

Compilation : `gradlew build` → `velocity/build/libs/EterTab-Velocity-<version>.jar` et `paper/build/libs/EterTab-Paper-<version>.jar`.
La version Velocity est aussi écrite dans `@Plugin` (`EterTabVelocity`) : à garder identique à `gradle.properties`.

## Côté Velocity

- **Indépendant d'EterLib** (qui est pour Paper) : config, langues et palette propres (`plugins/etertab/`), mêmes conventions
  (MiniMessage, `lang/<locale>.yml`, `en_us` par défaut, palette identique à `EterLib/config.yml`).
- Les serveurs Paper envoient leurs propres joueurs à la liste et la réinitialisent au changement de serveur : EterTab
  complète avec les joueurs des autres serveurs (profil et skin venant du proxy) et réécrit noms et ordre à chaque
  rafraîchissement (`update-interval`), plus 0,5 s après chaque connexion/changement de serveur.
- Seuls les joueurs ajoutés par EterTab sont retirés de la liste : les PNJ d'autres plugins ne sont pas touchés.
- LuckPerms (version Velocity) facultatif : sans lui, pas de grade et pas de tri par grade.

## Côté Paper

- Dépend d'**EterLib 1.2.0+** (langues, palette, `countOnline()` pour `<network>`).
- Un tableau de scores par joueur : sa sidebar (sans numéros rouges, seules les lignes modifiées sont renvoyées) et une
  équipe par joueur recopiée chez tous (grade au-dessus de la tête). Un autre plugin qui change le tableau d'un joueur
  remplace celui d'EterTab.
- Équipes nommées par poids de grade : sans proxy, le Tab de Minecraft est lui aussi trié par grade (`tab-order`).
- Facultatifs : LuckPerms (grades), Vault + EterEconomy (`<balance>`), PlaceholderAPI (`%variables%`). Soldes et total
  du réseau sont relus en tâche de fond toutes les 5 s, jamais pendant l'affichage.
