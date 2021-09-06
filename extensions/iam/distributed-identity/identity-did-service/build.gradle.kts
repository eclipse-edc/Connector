plugins {
    `java-library`
    `java-test-fixtures`
}


dependencies {
    api(project(":spi"))

    api(project(":extensions:iam:distributed-identity:identity-did-spi"))

    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
    implementation("com.nimbusds:nimbus-jose-jwt:8.20.1")

    testImplementation(testFixtures(project(":extensions:iam:distributed-identity:identity-common-test")))
}
