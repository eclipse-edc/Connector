plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:decentralized-claims-spi"))
    api(project(":spi:core-spi"))

    testImplementation(project(":core:common:junit"))
}

