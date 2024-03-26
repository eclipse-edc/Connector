plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:identity-trust-spi"))
    api(project(":spi:common:jwt-spi"))
    api(project(":spi:common:token-spi"))

    implementation(project(":core:common:util"))
    testImplementation(testFixtures(project(":spi:common:identity-trust-spi")))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:token-core"))
    testImplementation(libs.nimbus.jwt)
}

