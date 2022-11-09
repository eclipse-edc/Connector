plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))
    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
    api(libs.nimbus.jwt)
}

publishing {
    publications {
        create<MavenPublication>("identity-did-spi") {
            artifactId = "identity-did-spi"
            from(components["java"])
        }
    }
}
