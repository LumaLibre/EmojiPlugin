import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "net.lumamc.nexo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.nexomc.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.nexomc:nexo:1.16.1")
    compileOnly("net.luckperms:api:5.4")
}


tasks {
    runServer {
        minecraftVersion("1.21.11")
    }

    processResources {
        outputs.upToDateWhen { false }
        filter<ReplaceTokens>(mapOf(
            "tokens" to mapOf("version" to project.version.toString()),
            "beginToken" to "\${",
            "endToken" to "}"
        )).filteringCharset = "UTF-8"
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}