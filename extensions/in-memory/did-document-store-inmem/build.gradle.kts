plugins {
    `java-library`
}

val jwtVersion: String by project


dependencies {
    implementation(project(":extensions:iam:distributed-identity:identity-did-spi"))
}

publishing {
    publications {
        create<MavenPublication>("in-memory.did-document-store") {
            artifactId = "in-memory.did-document-store"
            from(components["java"])
        }
    }
}
