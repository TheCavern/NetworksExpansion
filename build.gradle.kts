import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gorylenko.GitPropertiesPluginExtension
import org.gradle.kotlin.dsl.named
import org.gradle.language.jvm.tasks.ProcessResources
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    `maven-publish`
    alias(libs.plugins.shadow)
    alias(libs.plugins.git.properties)
}

val buildTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM.dd.yyyy-HHmm")!!)!!

group = "com.ytdd9527.networksexpansion"
version = resolveVersion()
description = "NetworksExpansion"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(21)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://nexus.neetgames.com/repository/maven-public")
    maven("https://repo.bg-software.com/repository/api/")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven {
        url = URI("https://mvn.thecavern.net/slimefun")
        credentials {
            username = (project.findProperty("CavernUsername") ?: System.getenv("CAVERN_USERNAME")) as String?
            password = (project.findProperty("CavernPassword") ?: System.getenv("CAVERN_PASSWORD")) as String?
        }
    }
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.org.bstats.bstats.bukkit)
    implementation(libs.com.google.code.findbugs.annotations) {
        exclude(mapOf("group" to "net.jcip", "module" to "jcip-annotations"))
        exclude(mapOf("group" to "com.google.code.findbugs", "module" to "jsr305"))
    }

    implementation(libs.com.jeff.media.morepersistentdatatypes)
    implementation(libs.com.github.houbb.pinyin)
    implementation(libs.dev.sefiraat.sefilib)

    compileOnly(libs.io.papermc.paper.paper.api)
    compileOnly(libs.com.github.slimefun.slimefun)
    compileOnly(libs.com.github.slimefunguguproject.infinityexpansion)
    compileOnly(libs.com.github.sefiraat.netheopoiesis)
    compileOnly(libs.io.github.schntgaispock.slimehud)
    compileOnly(libs.com.bgsoftware.wildchestsapi)
    compileOnly(libs.com.bgsoftware.wildstackerapi)
    compileOnly(libs.dev.rosewood.rosestacker)

    compileOnly(libs.com.gmail.nossr50.mcmmo.mcmmo) {
        exclude(mapOf("group" to "com.sk89q.worldedit", "module" to "worldedit-bukkit"))
        exclude(mapOf("group" to "com.sk89q.worldedit", "module" to "worldedit-core"))
        exclude(mapOf("group" to "com.sk89q.worldguard", "module" to "worldguard-legacy"))
        exclude(mapOf("group" to "com.comphenix.protocol", "module" to "ProtocolLib"))
    }

    compileOnly(libs.net.guizhanss.guizhanlibplugin)
    compileOnly(libs.com.github.balugaq.fluffymachines)
    compileOnly(libs.com.github.timetowndev.guguslimefunlib)
    compileOnly(libs.com.github.balugaq.justenoughguide)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

tasks.jar {
    enabled = false
}

sourceSets.main {
    java.exclude("**/package-info.java")
}

configure<GitPropertiesPluginExtension> {
    keys = listOf(
        "git.build.time",
        "git.build.version",
        "git.commit.id.abbrev",
        "git.commit.id.full",
        "git.branch",
    )
    customProperty("git.build.version", version.toString())
    customProperty("git.build.time", buildTimestamp)
    gitPropertiesName = "git.properties"
    gitPropertiesResourceDir = layout.buildDirectory.dir("generated/git-properties").get().asFile
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(tasks.named("generateGitProperties"))
    val pluginVersion = version.toString()
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand(mapOf("version" to pluginVersion))
    }
}

tasks.named("sourcesJar") {
    dependsOn(tasks.named("generateGitProperties"))
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("NetworksExpansion")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    relocate("org.bstats", "io.github.sefiraat.networks.bstats")
    relocate("io.papermc.lib", "dev.sefiraat.cultivation.paperlib")
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.named("shadowJar"))
            artifact(tasks.named("sourcesJar"))
            groupId = project.group.toString()
            artifactId = "NetworksExpansion"
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "CavernMaven"
            url = uri("https://mvn.thecavern.net/slimefun")
            credentials {
                username = (project.findProperty("CavernUsername") ?: System.getenv("CAVERN_USERNAME")) as String?
                password = (project.findProperty("CavernPassword") ?: System.getenv("CAVERN_PASSWORD")) as String?
            }
        }
    }
}


fun Project.resolveVersion(): String {
    return "${findProperty("projectVersion")?.toString()?.takeIf { it.isNotBlank() } ?: "indev"}-$buildTimestamp"
}