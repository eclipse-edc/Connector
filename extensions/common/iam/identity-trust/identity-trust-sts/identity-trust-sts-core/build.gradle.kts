plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:common:identity-trust-spi"))
    api(project(":spi:common:identity-trust-sts-spi"))
    api(project(":spi:common:jwt-signer-spi"))

    implementation(project(":spi:common:keys-spi"))
    implementation(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-embedded"))
    implementation(project(":core:common:lib:token-lib"))
    implementation(project(":core:common:lib:store-lib"))

    testImplementation(testFixtures(project(":spi:common:identity-trust-sts-spi")))
    testImplementation(project(":core:common:lib:boot-lib"))
    testImplementation(project(":core:common:lib:crypto-common-lib"))
    testImplementation(project(":core:common:lib:keys-lib"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:query-lib"))
    testImplementation(libs.nimbus.jwt)
}
