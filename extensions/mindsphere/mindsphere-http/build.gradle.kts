plugins {
    `java-library`
}

val jodahFailsafeVersion: String by project
val okHttpVersion: String by project
val jacksonVersion: String by project
val mockitoVersion: String by project

dependencies {
    api(project(":spi"))
    api("net.jodah:failsafe:${jodahFailsafeVersion}")

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")

    testImplementation("com.squareup.okhttp3:mockwebserver:${okHttpVersion}")
    testImplementation("org.mockito:mockito-core:${mockitoVersion}")
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
