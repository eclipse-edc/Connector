plugins {
    `java-library`
}


dependencies {
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))
}

publishing {
    publications {
        create<MavenPublication>("in-mem.identity-hub") {
            artifactId = "in-mem.identity-hub"
            from(components["java"])
        }
    }
}
