plugins {
    `java-library`
}

dependencies {

    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))

    // this is required for the JcaPEMKeyConverter, which we use to restore keys from PEM files
    implementation("org.bouncycastle:bcpkix-jdk15on:1.56")
    testImplementation(project(":extensions:iam:decentralized-identity:identity-did-core"))

}
publishing {
    publications {
        create<MavenPublication>("dummy-credentials-verifier") {
            artifactId = "dummy-credentials-verifier"
            from(components["java"])
        }
    }
}
