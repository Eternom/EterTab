plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "EterTab"

// Un projet, deux jars : EterTab-Velocity (liste Tab du réseau) et EterTab-Paper (sidebar, noms au-dessus des têtes)
include("velocity", "paper")
