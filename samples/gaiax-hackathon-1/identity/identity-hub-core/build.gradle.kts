plugins {
    `java-library`
}

val rsApi: String by project

dependencies {
    api(project(":samples:gaiax-hackathon-1:identity:identity-hub-spi"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}
