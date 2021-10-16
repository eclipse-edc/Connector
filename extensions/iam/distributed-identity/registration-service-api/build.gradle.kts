plugins {
    `java-library`
}

val rsApi: String by project

dependencies {
    implementation(project(":common:util"))
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

}
publishing {
    publications {
        create<MavenPublication>("iam.registration-service-api") {
            artifactId = "iam.registration-service-api"
            from(components["java"])
        }
    }
}
