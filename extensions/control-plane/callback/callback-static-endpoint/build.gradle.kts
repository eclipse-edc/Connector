plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:control-plane-spi"))

    testImplementation(project(":core:common:junit"))
}
