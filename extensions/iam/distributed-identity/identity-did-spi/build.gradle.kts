plugins {
    `java-library`
}

//val jwtVersion: String by project

dependencies {
    api(project(":spi"))
//    implementation("com.auth0:java-jwt:${jwtVersion}")
}
