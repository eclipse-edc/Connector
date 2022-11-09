plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}


dependencies {
    api(project(":spi:common:identity-did-spi"))
    implementation(project(":extensions:common:iam:decentralized-identity:identity-did-crypto"))

    implementation(libs.jakarta.rsApi)
    implementation(libs.okhttp)

    testImplementation(testFixtures(project(":extensions:common:iam:decentralized-identity:identity-did-test")))
    testImplementation(project(":core:common:junit"))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-core") {
            artifactId = "identity-did-core"
            from(components["java"])
        }
    }
}
