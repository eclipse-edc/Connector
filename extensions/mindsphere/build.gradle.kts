plugins {
    `java-library`
}

//This file serves as BOM for all stores based on memory
dependencies {
    api(project(":spi"))
    api(project(":extensions:mindsphere:mindsphere-http"))
}

publishing {
    publications {
        create<MavenPublication>("mindsphere") {
            artifactId = "mindsphere"
            from(components["java"])
        }
    }
}