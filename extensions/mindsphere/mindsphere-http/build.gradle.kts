plugins {
    `java-library`
}

val jodahFailsafeVersion: String by project

dependencies {
    api(project(":spi"))

    api("net.jodah:failsafe:${jodahFailsafeVersion}")

    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("mindsphere-http") {
            artifactId = "mindsphere-http"
            from(components["java"])
        }
    }
}
