plugins {
    `java-library`
    `maven-publish`
}

description = "Public API for CelestiRift — register mechanics/targeters/conditions and listen to skill events."

java {
    withJavadocJar()
}

publishing {
    repositories {
        // Used by .github/workflows/release.yml. GITHUB_REPOSITORY is set by GitHub Actions.
        val repo = providers.environmentVariable("GITHUB_REPOSITORY")
        if (repo.isPresent) {
            maven("https://maven.pkg.github.com/${repo.get()}") {
                name = "GitHubPackages"
                credentials {
                    username = providers.environmentVariable("GITHUB_ACTOR").orNull
                    password = providers.environmentVariable("GITHUB_TOKEN").orNull
                }
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifactId = "celestirift-api"
            from(components["java"])
        }
    }
}

tasks.javadoc {
    // Keep doclint for broken references/HTML, but don't require a comment on every getter.
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}
