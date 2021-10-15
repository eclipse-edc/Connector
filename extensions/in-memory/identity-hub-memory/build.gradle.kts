plugins {
    `java-library`
}


dependencies {
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))
}

publishing {
    publications {
        create<MavenPublication>("in-memory.identity-hub") {
            artifactId = "in-memory.identity-hub"
            from(components["java"])
        }
    }
}
