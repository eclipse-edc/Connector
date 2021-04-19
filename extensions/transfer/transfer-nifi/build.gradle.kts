
plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    api(project(":extensions:transfer:transfer-types-aws"))
    api(project(":extensions:transfer:transfer-types-azure"))
    testImplementation("com.azure:azure-storage-blob:12.6.0")
}


