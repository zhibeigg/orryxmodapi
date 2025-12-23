@file:Suppress("PropertyName", "SpellCheckingInspection")

import io.izzel.taboolib.gradle.*

plugins {
    java
    kotlin("jvm") version "2.1.20"
    id("io.izzel.taboolib") version "2.0.27"
}

subprojects {
    apply<JavaPlugin>()
    apply(plugin = "io.izzel.taboolib")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    configure<TabooLibExtension> {
        env {
            install(Basic)
            install(Bukkit)
            install(BukkitUtil)
            install(BungeeCord)
            install(CommandHelper)
            install(BukkitNMS)
        }
        version { taboolib = "6.2.4-65252583" }
    }

    // 仓库
    repositories {
        mavenCentral()
    }

    // 依赖
    dependencies {
        compileOnly(kotlin("stdlib"))
        compileOnly(kotlin("reflect"))
    }

    kotlin {
        jvmToolchain(8)
    }

    java {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

gradle.buildFinished {
    buildDir.deleteRecursively()
}