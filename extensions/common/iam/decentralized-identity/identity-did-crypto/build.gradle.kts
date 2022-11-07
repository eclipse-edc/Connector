plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:identity-did-spi"))
    api(project(":spi:common:jwt-spi"))

    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    testImplementation(project(":core:common:junit"))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-crypto") {
            artifactId = "identity-did-crypto"
            from(components["java"])
        }
    }
}
