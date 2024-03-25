plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:keys-spi"))
    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
    api(libs.nimbus.jwt)
}


