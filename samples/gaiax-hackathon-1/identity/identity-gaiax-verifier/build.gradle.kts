plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))

    api(project(":samples:gaiax-hackathon-1:identity:identity-did-spi"))

    testImplementation(testFixtures(project(":samples:gaiax-hackathon-1:identity:identity-common-test")))

}
