plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    api("org.apache.atlas:atlas-client-v2:2.1.0")

    testImplementation(project(":distributions:junit"))
}
