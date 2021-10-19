plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))
}

publishing {
    publications {
        create<MavenPublication>("iam.identity-did-web") {
            artifactId = "iam.identity-did-web"
            from(components["java"])
        }
    }
}
