plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:decentralized-claims-spi"))
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))
    implementation(project(":core:common:lib:core-lib"))

    testImplementation(project(":core:common:junit"))
}

