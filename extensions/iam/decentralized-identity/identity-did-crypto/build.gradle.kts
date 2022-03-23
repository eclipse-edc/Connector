plugins {
    `java-library`
}

val nimbusVersion: String by project

dependencies {
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))

    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.56")
}

publishing {
    publications {
        create<MavenPublication>("identity-did-crypto") {
            artifactId = "identity-did-crypto"
            from(components["java"])
        }
    }
}
