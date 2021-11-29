plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))
    implementation(project(":extensions:iam:distributed-identity:identity-did-crypto"))

    testImplementation(testFixtures(project(":extensions:iam:distributed-identity:identity-common-test")))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-service") {
            artifactId = "identity-did-service"
            from(components["java"])
        }
    }
}
