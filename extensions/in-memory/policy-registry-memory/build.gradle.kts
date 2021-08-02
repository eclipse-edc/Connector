plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
}

publishing {
    publications {
        create<MavenPublication>("policy-mem") {
            artifactId = "edc.policy-registry-memory"
            from(components["java"])
        }
    }
}