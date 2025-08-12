plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:data-address:data-address-kafka-spi"))
    api(project(":spi:data-plane:data-plane-spi"))

    implementation(project(":core:common:lib:util-lib"))
    implementation(project(":core:common:lib:validator-lib"))
    implementation(project(":core:data-plane:data-plane-util"))
    implementation(project(":extensions:common:validator:validator-data-address-kafka"))
    implementation(libs.kafkaClients)

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
}
