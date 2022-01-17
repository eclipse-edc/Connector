plugins {
    `java-library`
}

val rsApi: String by project

dependencies {
    implementation(project(":common:util"))
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

}
publishing {
    publications {
        create<MavenPublication>("registration-service-api") {
            artifactId = "registration-service-api"
            from(components["java"])
        }
    }
}
