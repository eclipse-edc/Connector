plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:decentralized-claims-spi"))
    api(project(":spi:core-spi"))

    implementation(project(":core:common:lib:core-lib"))
    implementation(libs.nimbus.jwt)
    implementation(libs.iron.ed25519)
    testImplementation(testFixtures(project(":spi:decentralized-claims-spi")))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":spi:core-spi"))
    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(project(":core:common:lib:core-lib"))

    testImplementation(testFixtures(project(":spi:core-spi")))
    testImplementation(testFixtures(project(":extensions:common:crypto:ldp-verifiable-credentials")))
    testImplementation(testFixtures(project(":extensions:common:crypto:jwt-verifiable-credentials")))

}

