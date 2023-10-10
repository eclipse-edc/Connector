plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:identity-trust-spi"))
    implementation(project(":spi:common:http-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":extensions:common:crypto:jws2020"))
    implementation(project(":extensions:common:iam:identity-trust:identity-trust-service"))
    implementation(libs.nimbus.jwt)
    testImplementation(testFixtures(project(":spi:common:identity-trust-spi")))
    testImplementation(project(":core:common:junit"))
}

