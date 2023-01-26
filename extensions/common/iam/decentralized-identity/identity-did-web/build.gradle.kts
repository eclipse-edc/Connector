plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:common:identity-did-spi"))
    api(project(":spi:common:http-spi"))
    api(project(":core:common:util"))

    testImplementation(project(":core:common:junit"))
}


