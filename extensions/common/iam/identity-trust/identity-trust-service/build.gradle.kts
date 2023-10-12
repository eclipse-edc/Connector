plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:identity-trust-spi"))
    api(project(":spi:common:identity-did-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":extensions:common:iam:decentralized-identity:identity-did-crypto"))
    implementation(libs.nimbus.jwt)
    testImplementation(testFixtures(project(":spi:common:identity-trust-spi")))
    testImplementation(project(":core:common:junit"))
}

