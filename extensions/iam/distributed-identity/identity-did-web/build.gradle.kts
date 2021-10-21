plugins {
    `java-library`
    `java-test-fixtures`
}

val okHttpVersion: String by project

dependencies {
    api(project(":extensions:iam:distributed-identity:identity-did-spi"))

    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:${okHttpVersion}")
}

publishing {
    publications {
        create<MavenPublication>("iam.identity-did-web") {
            artifactId = "iam.identity-did-web"
            from(components["java"])
        }
    }
}
