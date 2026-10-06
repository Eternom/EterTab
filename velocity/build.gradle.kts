plugins {
    java
}

dependencies {
    // Fournit aussi Adventure/MiniMessage, SnakeYAML, Guice et SLF4J, présents sur le proxy
    compileOnly("com.velocitypowered:velocity-api:4.2.0")
    // Génère velocity-plugin.json à partir de l'annotation @Plugin
    annotationProcessor("com.velocitypowered:velocity-api:4.2.0")
    // Grades : fourni par le plugin LuckPerms installé sur le proxy (facultatif)
    compileOnly("net.luckperms:api:5.5")
}

tasks {
    jar {
        archiveBaseName = "EterTab-Velocity"
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}
