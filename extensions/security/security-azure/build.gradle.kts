val rsApi: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    implementation("com.azure:azure-security-keyvault-secrets:4.2.3")
    implementation("com.azure:azure-identity:1.2.0")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation("com.microsoft.azure:azure-mgmt-resources:1.3.0")
    testImplementation("com.azure.resourcemanager:azure-resourcemanager:2.1.0")
    testImplementation("com.azure:azure-identity:1.2.5")
    testImplementation("com.azure.resourcemanager:azure-resourcemanager-keyvault:2.2.0")
}


