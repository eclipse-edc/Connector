val infoModelVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    implementation(project(":data-protocols:ids:ids-spi"))
}


publishing {
    publications {
        create<MavenPublication>("data-protocols.ids-policy-mock") {
            artifactId = "data-protocols..ids-policy-mock"
            from(components["java"])
        }
    }
}
