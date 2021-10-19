plugins {
    `java-library`
}

dependencies {

    implementation(project(":extensions:iam:distributed-identity:identity-did-spi"))

    // this is required for the JcaPEMKeyConverter, which we use to restore keys from PEM files
    implementation("org.bouncycastle:bcpkix-jdk15on:1.56")
    testImplementation(project(":extensions:iam:distributed-identity:identity-did-core"))

}
publishing {
    publications {
        create<MavenPublication>("iam.dummy-credentials-verifier") {
            artifactId = "iam.dummy-credentials-verifier"
            from(components["java"])
        }
    }
}
