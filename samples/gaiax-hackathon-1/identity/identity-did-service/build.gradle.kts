plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))

    api(project(":samples:gaiax-hackathon-1:identity:identity-did-spi"))

    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
     implementation("com.nimbusds:nimbus-jose-jwt:8.20.1")

}
