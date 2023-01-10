plugins {
    `java-library`
}


dependencies {
    api(project(":spi:common:identity-did-spi"))
    implementation(project(":extensions:common:iam:decentralized-identity:identity-did-crypto"))

    implementation(libs.jakarta.rsApi)

    testImplementation(testFixtures(project(":extensions:common:iam:decentralized-identity:identity-did-test")))
    testImplementation(project(":core:common:junit"))
}


