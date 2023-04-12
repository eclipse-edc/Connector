plugins {
    `java-library`
    `maven-publish`
}


dependencies {
    api(project(":spi:data-plane:data-plane-spi"))
    api(project(":extensions:common:azure:azure-cosmos-core"))

    implementation(root.azure.cosmos)
    implementation(root.failsafe.core)

    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(testFixtures(project(":spi:data-plane:data-plane-spi")))

}


