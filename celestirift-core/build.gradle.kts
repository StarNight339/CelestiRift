import java.security.MessageDigest

plugins {
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
    id("com.modrinth.minotaur") version "2.10.0"
}

description = "CelestiRift Paper plugin."

dependencies {
    implementation(project(":celestirift-api"))
    // OrionRP API, compile-only (optional integration, softdepend; not bundled into the jar).
    // The jar lives in libs/ so anyone can build. To update it: run `./gradlew publishToMavenLocal`
    // in the OrionRP project and copy orionrp-api-<version>.jar from ~/.m2/repository/dev/orionrp/.
    compileOnly(files(rootProject.layout.projectDirectory.file(
        "libs/orionrp-api-${providers.gradleProperty("orionrpVersion").get()}.jar")))

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.papermc.paper:paper-api:${property("paperVersion")}")
}

tasks {
    jar {
        archiveBaseName.set("CelestiRift")
        // Bundle the API classes into the plugin jar (no extra shading tool needed).
        val api = project(":celestirift-api").sourceSets.main.get().output
        from(api)
        dependsOn(":celestirift-api:classes")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    processResources {
        val props = mapOf("version" to project.version.toString())
        inputs.properties(props)
        filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
            expand(props)
        }
    }

    test {
        useJUnitPlatform()
    }

    runServer {
        minecraftVersion(providers.gradleProperty("minecraftVersion").get())
        jvmArgs("-Dcom.mojang.eula.agree=true", "-DPaper.IgnoreJavaVersion=false")
        // Separate port so it can run next to a real server on 25565.
        args("--port", providers.gradleProperty("testServerPort").getOrElse("25570"), "--nogui")
    }
}

// ./gradlew smokeTest -> boots a throwaway Paper server (build/smoke-server) with the plugin,
// which casts every skill, spawns every boss, tests safe reload, writes smoke-result.txt and
// stops. The task fails if any check failed. Use -PpaperVersion / -PminecraftVersion to try
// another server version (e.g. the next Minecraft release).
val smokeTest = tasks.register<xyz.jpenilla.runpaper.task.RunServer>("smokeTest") {
    group = "verification"
    description = "Run the plugin's in-server self test on a fresh Paper server."
    minecraftVersion(providers.gradleProperty("minecraftVersion").get())
    pluginJars(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    runDirectory.set(layout.buildDirectory.dir("smoke-server"))
    jvmArgs("-Dcom.mojang.eula.agree=true", "-Dcelestirift.smokeTest=true")
    args("--port", "25571", "--nogui")
    val result = layout.buildDirectory.file("smoke-server/plugins/CelestiRift/smoke-result.txt")
    val serverDir = layout.buildDirectory.dir("smoke-server")
    doFirst {
        // Always start from the bundled defaults and a fresh world (the server jar cache stays).
        val dir = serverDir.get().asFile
        listOf("plugins/CelestiRift", "world", "world_nether", "world_the_end")
            .forEach { dir.resolve(it).deleteRecursively() }
    }
    doLast {
        val file = result.get().asFile
        if (!file.exists()) throw GradleException("Smoke test produced no result (server crashed?) - see output above.")
        val text = file.readText()
        println(text)
        if (!text.contains("RESULT PASS")) throw GradleException("Smoke test failed:\n$text")
    }
}

// ./gradlew deploy  ->builds and copies the plugin jar into a server's plugins folder.
// Set the folder once in gradle.properties (deployDir=...) or pass -PdeployDir=...
tasks.register<Copy>("deploy") {
    group = "celestirift"
    description = "Build the plugin and copy the jar into deployDir (a server plugins folder)."
    val dir = providers.gradleProperty("deployDir")
    onlyIf { dir.isPresent }
    from(tasks.named("jar"))
    into(dir.orElse("."))
}

// ./gradlew resourcePack -> build/resourcepack/CelestiRift-pack.zip (+ .sha1).
// The zip is also bundled into the plugin jar and extracted to plugins/CelestiRift/pack/.
val resourcePack = tasks.register<Zip>("resourcePack") {
    group = "celestirift"
    description = "Zip src/resourcepack into CelestiRift-pack.zip and print its SHA-1."
    from("src/resourcepack")
    archiveFileName.set("CelestiRift-pack.zip")
    destinationDirectory.set(layout.buildDirectory.dir("resourcepack"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    doLast {
        val zip = archiveFile.get().asFile
        val sha1 = MessageDigest.getInstance("SHA-1").digest(zip.readBytes())
            .joinToString("") { "%02x".format(it) }
        zip.resolveSibling(zip.name + ".sha1").writeText(sha1)
        logger.lifecycle("Resource pack: $zip\nSHA-1: $sha1")
    }
}

tasks.processResources {
    from(resourcePack) { into("pack") }
}

// ------------------------------------------------------------------ publishing
// Release: ./gradlew publishPluginPublicationToHangar modrinth -Pversion=2.0.0
// Tokens come from the environment: HANGAR_API_TOKEN, MODRINTH_TOKEN (see .github/workflows/release.yml).
// The changelog for the upload is the newest "## " section of CHANGELOG.md.
val releaseVersion = project.version.toString()
val isSnapshot = releaseVersion.contains("SNAPSHOT")
val supportedVersions = providers.gradleProperty("supportedMinecraftVersions").map { it.split(",").map(String::trim) }
val changelogText = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText.map<String> { text ->
    text.split(Regex("(?m)^## ")).drop(1).firstOrNull()?.substringAfter('\n')?.trim() ?: ""
}

hangarPublish {
    publications.register("plugin") {
        version.set(releaseVersion)
        channel.set(if (isSnapshot) "Snapshot" else "Release")
        id.set(providers.gradleProperty("hangarProject"))
        apiKey.set(providers.environmentVariable("HANGAR_API_TOKEN"))
        changelog.set(changelogText)
        platforms {
            register(io.papermc.hangarpublishplugin.model.Platforms.PAPER) {
                jar.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })
                platformVersions.set(supportedVersions)
            }
        }
    }
}

modrinth {
    token.set(providers.environmentVariable("MODRINTH_TOKEN"))
    projectId.set(providers.gradleProperty("modrinthProject"))
    versionNumber.set(releaseVersion)
    versionType.set(if (isSnapshot) "beta" else "release")
    uploadFile.set(tasks.named<Jar>("jar"))
    gameVersions.addAll(supportedVersions)
    loaders.addAll("paper", "purpur")
    changelog.set(changelogText)
}
