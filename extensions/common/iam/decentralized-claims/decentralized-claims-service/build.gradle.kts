plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:dcp-spi"))
    api(project(":spi:core-spi"))

    implementation(project(":core:common:lib:util-lib"))
    implementation(libs.nimbus.jwt)
    implementation(libs.iron.ed25519)
    testImplementation(testFixtures(project(":spi:dcp-spi")))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":spi:core-spi"))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":core:common:lib:crypto-common-lib"))

    testImplementation(testFixtures(project(":spi:core-spi")))
    testImplementation(testFixtures(project(":extensions:common:crypto:ldp-verifiable-credentials")))
    testImplementation(testFixtures(project(":extensions:common:crypto:jwt-verifiable-credentials")))

}

