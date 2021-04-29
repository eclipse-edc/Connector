
plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    testImplementation(project(":extensions:catalog:catalog-atlas"))
    testImplementation("com.azure:azure-storage-blob:12.6.0")
}


