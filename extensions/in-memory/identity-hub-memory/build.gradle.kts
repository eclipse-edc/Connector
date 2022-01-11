plugins {
    `java-library`
}


dependencies {
    api(project(":extensions:iam:decentralized-identifier:identity-did-spi"))
}

publishing {
    publications {
        create<MavenPublication>("identity-hub-memory") {
            artifactId = "identity-hub-memory"
            from(components["java"])
        }
    }
}
