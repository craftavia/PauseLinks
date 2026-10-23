plugins {
    java
}

group = "net.craftavia"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Built against the oldest supported API (1.21.6 added dialogs); runs on 1.21.6+ and 26.x.
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
    }

    compileTestJava {
        options.release.set(21)
    }

    processResources {
        inputs.property("version", project.version)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand("version" to project.version)
        }
    }

    test {
        useJUnitPlatform()
    }

    jar {
        archiveBaseName.set("PauseLinks")
        from("LICENSE")
    }
}
