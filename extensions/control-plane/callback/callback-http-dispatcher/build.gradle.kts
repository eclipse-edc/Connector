plugins {
    `java-library`
}



dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:http-spi"))


    testImplementation(root.mockserver.netty)
    testImplementation(root.mockserver.client)
    testImplementation(project(":core:common:junit"))

}
