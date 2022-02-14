plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

val rsApi: String by project
val okHttpVersion: String by project

dependencies {
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))
    implementation(project(":extensions:iam:decentralized-identity:identity-did-crypto"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    testImplementation(testFixtures(project(":extensions:iam:decentralized-identity:identity-common-test")))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-core") {
            artifactId = "identity-did-core"
            from(components["java"])
        }
    }
}
