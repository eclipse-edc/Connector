plugins {
    `java-library`
    `java-test-fixtures`
}

val okHttpVersion: String by project

dependencies {
    api(project(":spi:common:identity-did-spi"))
    api(project(":core:common:util"))

    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:${okHttpVersion}")
    testImplementation(project(":extensions:common:junit"))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-web") {
            artifactId = "identity-did-web"
            from(components["java"])
        }
    }
}
