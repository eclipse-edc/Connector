plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:common:identity-did-spi"))
    implementation(project(":extensions:common:iam:decentralized-identity:identity-did-crypto"))

    testImplementation(testFixtures(project(":extensions:common:iam:decentralized-identity:identity-did-test")))
    testImplementation(project(":extensions:common:junit"))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-service") {
            artifactId = "identity-did-service"
            from(components["java"])
        }
    }
}