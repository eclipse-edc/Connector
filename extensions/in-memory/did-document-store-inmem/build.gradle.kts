plugins {
    `java-library`
}

val jwtVersion: String by project


dependencies {
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))
}

publishing {
    publications {
        create<MavenPublication>("did-document-store-memory") {
            artifactId = "did-document-store-memory"
            from(components["java"])
        }
    }
}
