plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

val nimbusVersion: String by project
val rsApi: String by project
val okHttpVersion: String by project

dependencies {
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))
    implementation(project(":extensions:iam:decentralized-identity:identity-did-crypto"))

    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    testImplementation(testFixtures(project(":extensions:iam:decentralized-identity:identity-common-test")))
    testImplementation(testFixtures(project(":launchers:junit")))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-core") {
            artifactId = "identity-did-core"
            from(components["java"])
        }
    }
}
