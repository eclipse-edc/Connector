plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))

    implementation(project(":extensions:common:azure:azure-cosmos-core"))
    implementation(root.azure.cosmos)
    implementation(root.failsafe.core)

    testImplementation(testFixtures(project(":spi:data-plane-selector:data-plane-selector-spi")))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
}


