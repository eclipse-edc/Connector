plugins {
    `java-library`
    `maven-publish`
}

val cosmosSdkVersion: String by project
val failsafeVersion: String by project

dependencies {
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))
    api(project(":extensions:common:azure:azure-cosmos-core"))

    implementation("com.azure:azure-cosmos:${cosmosSdkVersion}")
    implementation("dev.failsafe:failsafe:${failsafeVersion}")

    testImplementation(testFixtures(project(":spi:data-plane-selector:data-plane-selector-spi")))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
}


publishing {
    publications {
        create<MavenPublication>("data-plane-instance-store-azure-cosmos") {
            artifactId = "data-plane-instance-store-azure-cosmos"
            from(components["java"])
        }
    }
}