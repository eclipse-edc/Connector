plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))

    implementation(project(":extensions:common:azure:azure-cosmos-core"))
    implementation(libs.azure.cosmos)
    implementation(libs.failsafe.core)

    testImplementation(testFixtures(project(":spi:data-plane-selector:data-plane-selector-spi")))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
