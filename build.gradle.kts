subprojects {
    apply(plugin = "java")

    repositories {
        // PaperMC en premier : il fournit aussi les dépendances courantes (et Velocity)
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(25)
    }
}
