@file:Suppress("PropertyName", "SpellCheckingInspection")

val publishUsername: String by project
val publishPassword: String by project
val build: String by project

plugins {
    `maven-publish`
}

taboolib {
    description {
        name = "OrryxModAPI"
        contributors {
            name("zhibei")
        }
    }
}

tasks {
    jar {
        // 构件名
        archiveBaseName.set(rootProject.name)
        // 打包子项目源代码
        rootProject.subprojects.forEach { from(it.sourceSets["main"].output) }
    }
    sourcesJar {
        // 构件名
        archiveBaseName.set(rootProject.name)
        // 打包子项目源代码
        rootProject.subprojects.forEach { from(it.sourceSets["main"].allSource) }
    }
}

tasks.withType<Jar> {
    destinationDirectory.set(file(build))
}

publishing {
    repositories {
        maven("https://nexus.mcwar.cn/repository/maven-releases/") {
            credentials {
                username = publishUsername
                password = publishPassword
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("api") {
            groupId = "org.minelegend.orryxmodapi"
            artifactId = "api"
            // 使用 taboolibBuildApi 任务的输出
            artifact("${build}/${rootProject.name}-${version}-api.jar")
            // 添加 sources jar
            artifact(tasks.named("kotlinSourcesJar")) {
                classifier = "sources"
            }
        }
    }
}