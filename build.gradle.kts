import io.izzel.taboolib.gradle.Basic
import io.izzel.taboolib.gradle.Bukkit
import io.izzel.taboolib.gradle.BukkitNMS

plugins {
    `java-library`
    kotlin("jvm") version "2.2.21"
    id("io.izzel.taboolib") version "2.0.27"
}

group = "org.minelegend.orryxmodapi"
version = "1.0-SNAPSHOT"

taboolib {
    env {
        install(Basic)
        install(Bukkit)
        install(BukkitNMS)
    }
    description {
        name = "OrryxModAPI"
        contributors {
            name("zhibei")
        }
    }
    version { taboolib = "6.2.4-3b3cd67" }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("ink.ptms.core:v11200:11200")
    compileOnly("ink.ptms:nms-all:1.0.0")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(8)
}

tasks.test {
    useJUnitPlatform()
}