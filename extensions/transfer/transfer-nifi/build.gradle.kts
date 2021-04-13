
plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    testImplementation("com.azure:azure-storage-blob:12.6.0")
}


