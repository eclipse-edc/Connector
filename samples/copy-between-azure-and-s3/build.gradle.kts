plugins {
    `java-library`
}

val jwtVersion: String by project


dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))

    implementation(project(":extensions:aws:s3:provision"))
    implementation(project(":extensions:azure:blob:api"))

}
