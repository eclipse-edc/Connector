plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:identity-trust-spi"))
    api(project(":spi:common:jwt-spi"))
    api(project(":spi:common:token-spi"))

    implementation(project(":core:common:lib:util-lib"))
    testImplementation(testFixtures(project(":spi:common:identity-trust-spi")))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:token-lib"))
    testImplementation(libs.nimbus.jwt)
}

