plugins {
    `java-library`
}

val jwtVersion: String by project


dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))
    implementation("com.auth0:java-jwt:${jwtVersion}")
    implementation("com.github.multiformats:java-multihash:1.2.0")

    api("com.nimbusds:nimbus-jose-jwt:8.20.1")
    implementation("com.google.crypto.tink:tink:1.6.1")
    implementation("io.github.erdtman:java-json-canonicalization:1.1")
    implementation("org.bouncycastle:bcprov-jdk15on:1.69") // for argon2id
}


publishing {
    publications {
        create<MavenPublication>("iam.iam-mock") {
            artifactId = "iam.iam-mock"
            from(components["java"])
        }
    }
}
