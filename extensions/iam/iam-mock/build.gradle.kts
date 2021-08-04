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
        create<MavenPublication>("iam.iam-mock") {
            artifactId = "iam.iam-mock"
            from(components["java"])
        }
    }
}
