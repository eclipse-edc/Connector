plugins {
    `java-library`
    `java-test-fixtures`
}

val nimbusVersion: String by project

dependencies {
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))
    implementation(project(":extensions:iam:decentralized-identity:identity-did-crypto"))
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")

    testImplementation(testFixtures(project(":extensions:iam:decentralized-identity:identity-common-test")))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-service") {
            artifactId = "identity-did-service"
            from(components["java"])
        }
    }
}
