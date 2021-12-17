plugins {
    `java-library`
}

val jwtVersion: String by project


dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))

    implementation(project(":extensions:aws:s3:writer"))
    implementation(project(":extensions:azure:blob:reader"))
    implementation(project(":extensions:inline-data-transfer:inline-data-transfer-core"))
    implementation(project(":extensions:in-memory:assetindex-memory"))

}
