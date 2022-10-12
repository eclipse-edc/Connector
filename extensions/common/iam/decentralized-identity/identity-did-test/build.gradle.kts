plugins {
    `java-library`
    `java-test-fixtures`
}


val nimbusVersion: String by project
dependencies {

    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
    testFixturesApi("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")

}

publishing {
    publications {
        create<MavenPublication>("identity-did-test") {
            artifactId = "identity-did-test"
            from(components["java"])
        }
    }
}