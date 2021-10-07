plugins {
    `java-library`
}

val jwtVersion: String by project


dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))
    implementation(project(":extensions:iam:distributed-identity:identity-did-spi"))
    testImplementation(project(":extensions:iam:distributed-identity:identity-did-core")) // for the KeyPairFactory

}

publishing {
    publications {
        create<MavenPublication>("in-mem.did-document-store") {
            artifactId = "in-mem.did-document-store"
            from(components["java"])
        }
    }
}
