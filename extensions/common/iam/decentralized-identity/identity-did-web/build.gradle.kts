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
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
