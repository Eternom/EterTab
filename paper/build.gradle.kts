plugins {
    java
}

repositories {
    // EterLib et VaultAPI : compilés depuis GitHub
    maven("https://jitpack.io")
    // PlaceholderAPI
    maven("https://repo.extendedclip.com/releases/")
    // Repli : EterLib publié sur cette machine (`gradlew publishToMavenLocal` dans EterLib)
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")
    // Socle commun : base, langues, palette, joueurs du réseau (plugin EterLib installé sur le serveur)
    compileOnly("com.github.Eternom:EterLib:1.2.0")
    // Grades (LuckPerms), solde (Vault -> EterEconomy), variables d'autres plugins (PlaceholderAPI) : tous facultatifs
    compileOnly("net.luckperms:api:5.5")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.12.3")
}

tasks {
    jar {
        archiveBaseName = "EterTab-Paper"
    }
    processResources {
        val props = mapOf("version" to version)
        // Déclarée comme entrée : sinon le cache de Gradle réutilise un plugin.yml avec l'ancienne version
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}
