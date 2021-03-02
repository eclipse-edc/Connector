plugins {
    `java-library`
}

val jwtVersion: String by project
val okHttpVersion: String by project

dependencies {
    api(project(":spi"))
    implementation("com.auth0:java-jwt:${jwtVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
}


