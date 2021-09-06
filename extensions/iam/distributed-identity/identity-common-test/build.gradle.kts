plugins {
    `java-library`
    `java-test-fixtures`
}


dependencies {

    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
    testFixturesApi("com.nimbusds:nimbus-jose-jwt:8.20.1")

}
