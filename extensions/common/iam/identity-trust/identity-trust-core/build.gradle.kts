plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:identity-trust-spi"))
    implementation(project(":spi:common:http-spi"))
    implementation(project(":spi:common:json-ld-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":core:common:jwt-core"))
    implementation(project(":extensions:common:crypto:jws2020"))
    implementation(project(":extensions:common:crypto:jwt-verifiable-credentials"))
    implementation(project(":extensions:common:crypto:ldp-verifiable-credentials"))
    implementation(project(":extensions:common:crypto:crypto-core"))
    implementation(project(":extensions:common:iam:identity-trust:identity-trust-service"))
    implementation(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-embedded"))
    implementation(libs.nimbus.jwt)

    testImplementation(testFixtures(project(":spi:common:identity-trust-spi")))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.nimbus.jwt)
}

