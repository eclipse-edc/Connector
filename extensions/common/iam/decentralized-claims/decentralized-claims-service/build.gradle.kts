plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:decentralized-claims-spi"))
    api(project(":spi:common:identity-did-spi"))
    api(project(":spi:common:jwt-spi"))
    api(project(":spi:common:token-spi"))

    implementation(project(":core:common:lib:util-lib"))
    implementation(libs.nimbus.jwt)
    implementation(libs.iron.ed25519)
    testImplementation(testFixtures(project(":spi:common:decentralized-claims-spi")))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":spi:common:json-ld-spi"))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":core:common:lib:crypto-common-lib"))

    testImplementation(testFixtures(project(":spi:common:verifiable-credentials-spi")))
    testImplementation(testFixtures(project(":extensions:common:crypto:ldp-verifiable-credentials")))
    testImplementation(testFixtures(project(":extensions:common:crypto:jwt-verifiable-credentials")))

}

