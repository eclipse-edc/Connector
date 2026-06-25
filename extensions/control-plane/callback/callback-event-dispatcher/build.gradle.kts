plugins {
    `java-library`
}

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":core:common:lib:core-lib")))
    testImplementation(libs.wiremock)
}
