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
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("com.gorylenko.gradle-git-properties") version "2.5.7"
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
    maven("https://nexus.neetgames.com/repository/maven-public")
    maven("https://repo.bg-software.com/repository/api/")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.alessiodp.com/releases/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.alessiodp.com/releases")
    maven("https://repo.jeff-media.com/public")
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
    // Core
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.slimefun:Slimefun:indev-Jul.06.2026-1159")

    // Tools etc.
    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation("com.jeff-media:MorePersistentDataTypes:2.4.0")
    implementation("dev.sefiraat:SefiLib:0.2.6")
    implementation("net.byteflux:libby-bukkit:1.3.2")

    compileOnly("com.google.code.findbugs:annotations:3.0.1u2") {
        exclude("net.jcip", "jcip-annotations")
        exclude("com.google.code.findbugs", "jsr305")
    }
    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
    compileOnly("com.github.houbb:pinyin:0.4.0")

    // Supported Plugins
    compileOnly("com.github.SlimefunGuguProject:InfinityExpansion:3c5db3650a")
    compileOnly("com.github.Sefiraat:Netheopoiesis:8d1af6c570")
    compileOnly("io.github.schntgaispock.slimehud:SlimeHUD:SlimeHUD:indev-Jul.06.2026-1431")
    compileOnly("com.bgsoftware:WildChestsAPI:2026.2")
    compileOnly("com.bgsoftware:WildStackerAPI:2026.2")
    compileOnly("dev.rosewood:rosestacker:1.5.23")
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.3.000") {
        exclude("com.sk89q.worldedit", "worldedit-bukkit")
        exclude("com.sk89q.worldedit", "worldedit-core")
        exclude("com.sk89q.worldguard", "worldguard-legacy")
        exclude("com.comphenix.protocol", "ProtocolLib")
    }
    compileOnly("net.guizhanss:GuizhanLibPlugin:2.5.0-Cavern")
    compileOnly("com.github.balugaq:FluffyMachines:indev-Jul.06.2026-1252")
    compileOnly("com.github.TimetownDev:GuguSlimefunLib:45627c6f8e")
    compileOnly("com.github.balugaq:JustEnoughGuide:7f21e113a2")
    // System-scoped local JARs
    compileOnly(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar"))))
}

tasks.withType<JavaExec>().configureEach {
    systemProperty("file.encoding", "UTF-8")
    systemProperty("sun.stdout.encoding", "UTF-8")
    systemProperty("sun.stderr.encoding", "UTF-8")
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
        minimize()

        // Relocations
        relocate("org.bstats", "io.github.sefiraat.networks.bstats")
        relocate("io.papermc.lib", "dev.sefiraat.cultivation.paperlib")
        relocate("net.byteflux.libby", "com.balugaq.netex.libraries.libby")

        // Exclude META-INF
        exclude("META-INF/*")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        mergeServiceFiles()
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

tasks.runServer {
        dependsOn(shadowJar)
        val run = file(providers.gradleProperty("server.run.dir").orElse("run"))
        runDirectory.set(run)

        doFirst {
            run.resolve("eula.txt").writeText("eula=true")

            val pl = run.resolve("plugins")
            pl.mkdirs()
            copy {
                from(projectDir.resolve("build/libs")) {
                    include("${name}-${version}.jar")
                }
                into(pl)
            }
        }

        jvmArgs(
            "-Dfile.encoding=UTF-8",
            "-Dsun.jnu.encoding=UTF-8",
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5001",
            "-Dnet.kyori.adventure.text.warn_when_legacy_formatting_detected=false"
        )
        maxHeapSize = "4G"
        minecraftVersion("1.20.1")
}

defaultTasks("clean", "build")

fun Project.resolveVersion(): String {
    return "${findProperty("projectVersion")?.toString()?.takeIf { it.isNotBlank() } ?: "indev"}-$buildTimestamp"
}