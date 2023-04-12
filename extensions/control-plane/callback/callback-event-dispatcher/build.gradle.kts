plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))

    testImplementation(project(":core:common:junit"))
}
