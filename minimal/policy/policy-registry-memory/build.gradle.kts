plugins {
    `java-library`
}


dependencies {
    api(project(":edc-core:spi"))
}

publishing {
    publications {
        create<MavenPublication>("policy-mem") {
            artifactId = "edc.policy-registry-memory"
            from(components["java"])
        }
    }
}