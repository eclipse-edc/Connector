
plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    implementation(project(":extensions:schema"))

    testImplementation(project(":extensions:catalog:catalog-atlas"))
    testImplementation("com.azure:azure-storage-blob:12.6.0")
    testImplementation(project(":extensions:catalog:catalog-dataseed"))
}


