plugins {
    `java-library`
}

val nimbusVersion: String by project
dependencies {
    api(project(":spi"))
    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
}

publishing {
    publications {
        create<MavenPublication>("iam.identity-did-spi") {
            artifactId = "iam.identity-did-spi"
            from(components["java"])
        }
    }
}