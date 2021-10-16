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
        create<MavenPublication>("iam.identity-did-service") {
            artifactId = "iam.identity-did-service"
            from(components["java"])
        }
    }
}
