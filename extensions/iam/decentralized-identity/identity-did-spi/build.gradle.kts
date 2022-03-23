plugins {
    `java-library`
}

val nimbusVersion: String by project

dependencies {
    api(project(":spi"))
    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
}

publishing {
    publications {
        create<MavenPublication>("identity-did-spi") {
            artifactId = "identity-did-spi"
            from(components["java"])
        }
    }
}
