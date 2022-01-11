plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":extensions:iam:decentralized-identifier:identity-did-spi"))
    implementation(project(":extensions:iam:decentralized-identifier:identity-did-crypto"))

    testImplementation(testFixtures(project(":extensions:iam:decentralized-identifier:identity-common-test")))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-service") {
            artifactId = "identity-did-service"
            from(components["java"])
        }
    }
}
