plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:identity-trust-spi"))
    api(project(":spi:common:identity-did-spi"))
    implementation(project(":extensions:common:crypto:jws2020"))
    implementation(project(":extensions:common:crypto:ldp-verifiable-credentials"))
    implementation(project(":extensions:common:crypto:jwt-verifiable-credentials"))

    implementation(project(":core:common:util"))
    implementation(libs.nimbus.jwt)
    implementation(libs.iron.ed25519)
    testImplementation(testFixtures(project(":spi:common:identity-trust-spi")))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(project(":extensions:common:crypto:crypto-core"))

    implementation(testFixtures(project(":extensions:common:crypto:ldp-verifiable-credentials")))
    implementation(testFixtures(project(":extensions:common:crypto:jwt-verifiable-credentials")))

}

