plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))
    api(project(":data-protocols:ion:ion-core"))
}
publishing {
    publications {
        create<MavenPublication>("iam.identity-hub-verifier") {
            artifactId = "iam.identity-hub-verifier"
            from(components["java"])
        }
    }
}