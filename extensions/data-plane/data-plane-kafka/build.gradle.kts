plugins {
    `java-library`
}

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:data-plane-spi"))

    implementation(project(":core:common:lib:core-lib"))
    implementation(project(":core:common:lib:jsonld-lib"))
    implementation(project(":core:data-plane:data-plane-util"))
    implementation(project(":extensions:common:validator:validator-data-address-kafka"))
    implementation(libs.kafkaClients)

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
}
