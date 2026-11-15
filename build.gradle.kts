subprojects {
    apply(plugin = "java")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    }

    dependencies {
        "compileOnly"("io.papermc.paper:paper-api:${property("paperVersion")}")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
        if (providers.gradleProperty("strictDeprecation").getOrElse("true").toBoolean()) {
            options.compilerArgs.add("-Werror")
        }
    }
}
