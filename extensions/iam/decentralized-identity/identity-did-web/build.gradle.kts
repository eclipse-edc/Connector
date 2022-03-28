plugins {
    `java-library`
    `java-test-fixtures`
}

val okHttpVersion: String by project

dependencies {
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))
    api(project(":common:util"))

    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:${okHttpVersion}")
    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-web") {
            artifactId = "identity-did-web"
            from(components["java"])
        }
    }
}
