plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:identity-did-spi"))

    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    testImplementation(project(":extensions:common:junit"))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-crypto") {
            artifactId = "identity-did-crypto"
            from(components["java"])
        }
    }
}
