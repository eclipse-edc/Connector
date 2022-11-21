plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:common:identity-did-spi"))
    api(project(":core:common:util"))

    implementation(libs.dnsOverHttps)
    testImplementation(project(":core:common:junit"))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-web") {
            artifactId = "identity-did-web"
            from(components["java"])
        }
    }
}
