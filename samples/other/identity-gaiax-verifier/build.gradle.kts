plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))

    api(project(":extensions:iam:distributed-identity:identity-did-spi"))

    testImplementation(testFixtures(project(":extensions:iam:distributed-identity:identity-common-test")))

}
