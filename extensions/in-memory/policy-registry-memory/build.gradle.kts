plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
}

publishing {
    publications {
        create<MavenPublication>("in-mem.policy-registry") {
            artifactId = "in-mem.policy-registry"
            from(components["java"])
        }
    }
}
