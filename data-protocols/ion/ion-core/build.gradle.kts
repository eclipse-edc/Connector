plugins {
    `java-library`
}

val jwtVersion: String by project
val okHttpVersion: String by project

repositories {
    mavenCentral()
}

dependencies {

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("com.auth0:java-jwt:${jwtVersion}")

    api("com.nimbusds:nimbus-jose-jwt:8.20.1")
    implementation("com.google.crypto.tink:tink:1.6.1")
    implementation("io.github.erdtman:java-json-canonicalization:1.1")
    implementation("org.bouncycastle:bcprov-jdk15on:1.69") // for argon2id

    testImplementation(testFixtures(project(":common:util")))
}
