plugins {
    `java-library`
}

val jwtVersion: String by project

dependencies {
    api(project(":spi"))
    implementation("com.auth0:java-jwt:${jwtVersion}")
}


publishing {
    publications {
        create<MavenPublication>("iam-mock") {
            artifactId = "dataspaceconnector.iam-mock"
            from(components["java"])
        }
    }
}
