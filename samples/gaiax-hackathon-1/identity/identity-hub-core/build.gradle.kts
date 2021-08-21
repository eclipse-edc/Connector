plugins {
    `java-library`
}

val rsApi: String by project

dependencies {
    api(project(":samples:gaiax-hackathon-1:identity:identity-hub-spi"))

    implementation("com.nimbusds:nimbus-jose-jwt:9.12.1")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}
