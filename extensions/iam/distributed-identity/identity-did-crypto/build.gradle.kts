plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))

    implementation("org.bouncycastle:bcpkix-jdk15on:1.56")
}

publishing {
    publications {
        create<MavenPublication>("identity-did-core") {
            artifactId = "identity-did-core"
            from(components["java"])
        }
    }
}
