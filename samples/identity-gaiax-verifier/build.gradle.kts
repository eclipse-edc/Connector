plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))

    api(project(":extensions:iam:distributed-identity:identity-did-spi"))

    testImplementation(testFixtures(project(":samples:gaiax-hackathon-1:identity:identity-common-test")))

}
