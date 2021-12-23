plugins {
    `java-library`
}

val jwtVersion: String by project

val nimbusVersion: String by project

dependencies {
    api(project(":core:base"))
    api(project(":extensions:ion:ion-core"))

    implementation("org.bouncycastle:bcprov-jdk15on:1.69") // for argon2id

    testImplementation(project(":extensions:iam:distributed-identity:identity-did-crypto")) // for the KeyPairFactory
}

publishing {
    publications {
        create<MavenPublication>("ion-client") {
            artifactId = "ion-client"
            from(components["java"])
        }
    }
}
