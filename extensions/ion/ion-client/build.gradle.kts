plugins {
    `java-library`
}

val jwtVersion: String by project
val okHttpVersion: String by project
val nimbusVersion: String by project

dependencies {
    api(project(":extensions:ion:ion-core"))

    implementation("org.bouncycastle:bcprov-jdk15on:1.69") // for argon2id
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    testImplementation(project(":extensions:iam:decentralized-identity:identity-did-crypto")) // for the KeyPairFactory
}

publishing {
    publications {
        create<MavenPublication>("ion-client") {
            artifactId = "ion-client"
            from(components["java"])
        }
    }
}
