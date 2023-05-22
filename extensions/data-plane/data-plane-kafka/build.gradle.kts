plugins {
    `java-library`
}

dependencies {
    api(project(":spi:data-plane:data-plane-spi"))

    implementation(project(":core:common:util"))
    implementation(project(":core:data-plane:data-plane-util"))
    implementation(libs.kafkaClients)

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.mockserver.netty)
    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
}
