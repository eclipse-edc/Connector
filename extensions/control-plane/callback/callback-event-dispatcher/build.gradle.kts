plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:http-spi"))
    api(project(":spi:control-plane:control-plane-spi"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":core:common:lib:http-lib")))
    testImplementation(libs.wiremock)
}
