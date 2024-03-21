plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:common:identity-trust-spi"))
    api(project(":spi:common:identity-trust-sts-spi"))


    implementation(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-embedded"))
    implementation(project(":core:common:token-core"))

    testImplementation(project(":core:common:connector-core")) // TEMPORARY - REMOVE
    testImplementation(testFixtures(project(":spi:common:identity-trust-sts-spi")))
    testImplementation(project(":core:common:lib:boot-lib"))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.nimbus.jwt)
}
