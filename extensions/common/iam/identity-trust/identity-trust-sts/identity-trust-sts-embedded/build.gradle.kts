plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:identity-trust-spi"))
    api(project(":spi:common:jwt-spi"))

    implementation(project(":core:common:util"))
    implementation(project(":extensions:common:iam:identity-trust:identity-trust-service"))
    testImplementation(testFixtures(project(":spi:common:identity-trust-spi")))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:jwt-core"))
    testImplementation(libs.nimbus.jwt)
}

