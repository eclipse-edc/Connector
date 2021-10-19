plugins {
    `java-library`
}

val jwtVersion: String by project

val nimbusVersion: String by project

dependencies {
    api(project(":extensions:ion:ion-core"))

    implementation("org.bouncycastle:bcprov-jdk15on:1.69") // for argon2id

    testImplementation(project(":extensions:iam:distributed-identity:identity-did-crypto")) // for the KeyPairFactory
}

publishing {
    publications {
        create<MavenPublication>("ion.ion-client") {
            artifactId = "ion.ion-client"
            from(components["java"])
        }
    }
}
