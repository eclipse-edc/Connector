val infoModelVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    api("org.slf4j:slf4j-api:2.0.0-alpha1")
}

