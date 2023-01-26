plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:common:identity-did-spi"))
    implementation(project(":extensions:common:iam:decentralized-identity:identity-did-crypto"))

    testImplementation(testFixtures(project(":extensions:common:iam:decentralized-identity:identity-did-test")))
    testImplementation(project(":core:common:junit"))
}


