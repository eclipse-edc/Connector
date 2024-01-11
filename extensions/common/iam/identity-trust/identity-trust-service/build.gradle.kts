plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:identity-trust-spi"))
    api(project(":spi:common:identity-did-spi"))
    api(project(":spi:common:jwt-spi"))

    implementation(project(":core:common:util"))
    implementation(libs.nimbus.jwt)
    implementation(libs.iron.ed25519)
    testImplementation(testFixtures(project(":spi:common:identity-trust-spi")))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(project(":extensions:common:crypto:crypto-common"))

    testImplementation(testFixtures(project(":extensions:common:crypto:ldp-verifiable-credentials")))
    testImplementation(testFixtures(project(":extensions:common:crypto:jwt-verifiable-credentials")))

}

