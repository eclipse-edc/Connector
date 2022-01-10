plugins {
    `java-library`
}

val rsApi: String by project

dependencies {
    api(project(":core:base"))
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))
    implementation(project(":extensions:iam:distributed-identity:identity-did-crypto"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(testFixtures(project(":extensions:iam:distributed-identity:identity-common-test")))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-core") {
            artifactId = "identity-did-core"
            from(components["java"])
        }
    }
}
