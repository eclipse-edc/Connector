plugins {
    `java-library`
}



dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:http-spi"))
    api(project(":spi:control-plane:control-plane-spi"))


    testImplementation(libs.wiremock)
    testImplementation(testFixtures(project(":core:common:lib:http-lib")))
    testImplementation(project(":core:common:junit"))

}
