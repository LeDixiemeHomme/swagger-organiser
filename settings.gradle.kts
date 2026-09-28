pluginManagement {
    repositories {
        maven {
            name = "nexusPicV3"
            url = uri("https://nexus-picv3-r.assurances.group.gca/repository/maven-public/")
        }
        gradlePluginPortal()
    }
}

rootProject.name = "swagger-organiser"