plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:data-address:data-address-kafka-spi"))
    api(project(":spi:data-plane:data-plane-spi"))

    implementation(project(":core:common:util"))
    implementation(project(":core:common:validator-lib"))
    implementation(project(":core:data-plane:data-plane-util"))
    implementation(project(":extensions:common:validator:validator-data-address-kafka"))
    implementation(libs.kafkaClients)

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
}
