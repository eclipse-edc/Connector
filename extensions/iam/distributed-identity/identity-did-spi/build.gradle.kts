plugins {
    `java-library`
}

val nimbusVersion: String by project
dependencies {
    api(project(":spi"))
    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
    api("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
}

publishing {
    publications {
        create<MavenPublication>("iam.identity-did-spi") {
            artifactId = "iam.identity-did-spi"
            from(components["java"])
        }
    }
}
