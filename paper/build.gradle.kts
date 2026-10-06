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
    compileOnly("com.github.Eternom:EterLib:1.5.0")
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

// Code partagé par les deux jars (grades LuckPerms, animations) : compilé dans chacun
sourceSets {
    main {
        java.srcDir("../common/src/main/java")
    }
}

// Après chaque build, copie le jar dans <eterPluginsDir>/paper et y supprime l'ancienne version de ce plugin.
// eterPluginsDir se règle dans ~/.gradle/gradle.properties (propre à ta machine) : sans lui, rien n'est copié (JitPack...).
val deployPlugin by tasks.registering(Copy::class) {
    description = "Copie le jar dans le dossier de plugins local (eterPluginsDir)"
    // Variables locales à la tâche : le cache de configuration de Gradle refuse les variables du script
    val eterPluginsDir = providers.gradleProperty("eterPluginsDir")
    val enabled = eterPluginsDir.isPresent
    onlyIf { enabled }
    val jarName = tasks.jar.flatMap { it.archiveBaseName }
    from(tasks.jar)
    into(eterPluginsDir.map { "$it/paper" }.orElse(layout.buildDirectory.dir("deploy").map { it.asFile.path }))
    doFirst {
        destinationDir.listFiles { file -> file.name.startsWith(jarName.get() + "-") && file.name.endsWith(".jar") }
            ?.forEach { it.delete() }
    }
}
tasks.build { finalizedBy(deployPlugin) }
