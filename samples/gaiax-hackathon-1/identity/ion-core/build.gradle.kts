plugins {
    `java-library`
}

val jwtVersion: String by project
val okHttpVersion: String by project

dependencies {

    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("com.auth0:java-jwt:${jwtVersion}")
    implementation("com.github.multiformats:java-multihash:1.2.0")

    api("com.nimbusds:nimbus-jose-jwt:8.20.1")
    implementation("com.google.crypto.tink:tink:1.6.1")
    implementation("io.github.erdtman:java-json-canonicalization:1.1")
    implementation("org.bouncycastle:bcprov-jdk15on:1.69") // for argon2id

    testImplementation(testFixtures(project(":common:util")))
}
