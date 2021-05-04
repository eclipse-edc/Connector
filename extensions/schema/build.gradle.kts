plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))

    testImplementation(project(":distributions:junit"))
}


