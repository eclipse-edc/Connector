plugins {
    `java-library`
}

val jwtVersion: String by project


dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))
    implementation(project(":data-protocols:ion:ion-core"))
    implementation(project(":extensions:iam:distributed-identity:identity-did-core"))

}

publishing {
    publications {
        create<MavenPublication>("in-mem.did-document-store") {
            artifactId = "in-mem.did-document-store"
            from(components["java"])
        }
    }
}
