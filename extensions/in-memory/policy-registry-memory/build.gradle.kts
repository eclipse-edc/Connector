plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
}

publishing {
    publications {
        create<MavenPublication>("policy-mem") {
            artifactId = "dataspaceconnector.policy-registry-memory"
            from(components["java"])
        }
    }
}
