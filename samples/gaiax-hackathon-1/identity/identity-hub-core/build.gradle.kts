plugins {
    `java-library`
}

val rsApi: String by project

dependencies {
    api(project(":samples:gaiax-hackathon-1:identity:identity-hub-spi"))

    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
    implementation("com.nimbusds:nimbus-jose-jwt:8.20.1")

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}
