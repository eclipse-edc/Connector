plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:decentralized-claims-spi"))
    api(project(":spi:common:oauth2-spi"))
    api(project(":spi:common:jwt-spi"))

    testImplementation(project(":core:common:junit"))
}

