plugins {
    `java-library`
}

val jwtVersion: String by project
val rsApi: String by project
val okHttpVersion: String by project

dependencies {
    api(project(":extensions:azure:events-config"))
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))

    // third party
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    api("org.quartz-scheduler:quartz:2.3.0")
}

publishing {
    publications {
        create<MavenPublication>("registration-service") {
            artifactId = "registration-service"
            from(components["java"])
        }
    }
}
