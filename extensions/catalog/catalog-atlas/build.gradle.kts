plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    implementation("org.apache.atlas:atlas-client-v2:2.1.0")
}
