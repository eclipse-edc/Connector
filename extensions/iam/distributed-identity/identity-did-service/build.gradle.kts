plugins {
    `java-library`
    `java-test-fixtures`
}

val nimbusVersion: String by project

dependencies {
    api(project(":spi"))

    api(project(":extensions:iam:distributed-identity:identity-did-spi"))

    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")

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