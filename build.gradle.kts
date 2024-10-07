plugins {
    `java-library`
    id("java")
    id("maven-publish")
    id("io.papermc.paperweight.userdev") version "1.7.3"
    id("com.gradleup.shadow") version "8.3.1"
}

group = "ru.violence"
version = "1.0.2"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("org.graalvm.polyglot:polyglot:24.0.2")
    implementation("org.graalvm.polyglot:js-community:24.0.2")
    implementation("dev.jorel:commandapi-bukkit-shade:9.5.3")
    compileOnly("org.jetbrains:annotations:24.1.0")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
    
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "version" to project.version,
            "apiVersion" to "1.21"
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        relocate("dev.jorel.commandapi", "ru.violence.graaljs.shaded.dev.jorel.commandapi")
    }

    build {
        dependsOn(shadowJar)
    }

    reobfJar {
        outputJar.set(layout.buildDirectory.file("libs/GraalJS.jar"))
    }
}
