plugins {
    `java-library`
}

val rsApi: String by project

dependencies {
    api(project(":spi"))
    api(project(":core"))

    implementation(project(":common:util"))

    implementation(project(":extensions:mindsphere:mindsphere-http"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}
