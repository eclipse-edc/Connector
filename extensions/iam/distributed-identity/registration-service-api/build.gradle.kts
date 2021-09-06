plugins {
    `java-library`
}

val jwtVersion: String by project
val rsApi: String by project


dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))
    implementation(project(":data-protocols:ion:ion-core"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

}
