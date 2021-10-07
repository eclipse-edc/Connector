plugins {
    `java-library`
}

val rsApi: String by project
val nimbusVersion: String by project

dependencies {
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))
    api(project(":extensions:ion:ion-core"))

    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(testFixtures(project(":extensions:iam:distributed-identity:identity-common-test")))
}

publishing {
    publications {
        create<MavenPublication>("iam.identity-did-core") {
            artifactId = "iam.identity-did-core"
            from(components["java"])
        }
    }
}
