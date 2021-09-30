plugins {
    `java-library`
}

val nimbusVersion: String by project
dependencies {

    api(project(":spi"))
    implementation(project(":extensions:ion:ion-core"))
    implementation(project(":extensions:iam:distributed-identity:identity-did-spi"))

    api("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
    // this is required for the JcaPEMKeyConverter, which we use to restore keys from PEM files
    implementation("org.bouncycastle:bcpkix-jdk15on:1.56")
    testImplementation(project(":extensions:iam:distributed-identity:identity-did-core"))

}
publishing {
    publications {
        create<MavenPublication>("iam.verifiable-credentials") {
            artifactId = "iam.verifiable-credentials"
            from(components["java"])
        }
    }
}