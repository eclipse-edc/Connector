plugins {
    `java-library`
}

val jwtVersion: String by project


dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))

    implementation(project(":extensions:aws:s3:provision"))
    implementation(project(":extensions:azure:blob:api"))
    implementation(project(":extensions:in-memory:assetindex-memory"))
    implementation(project(":extensions:in-memory:dataaddress-resolver-memory"))

}
