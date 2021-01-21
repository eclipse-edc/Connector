val infoModelVersion: String by project
val servletApi: String by project
val rsApi: String by project
val jettyVersion: String by project
val jerseyVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

}


