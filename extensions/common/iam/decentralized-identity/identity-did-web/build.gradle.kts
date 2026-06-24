plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:core-spi"))
    api(project(":core:common:lib:util-lib"))

    testImplementation(testFixtures(project(":core:common:lib:http-lib")))
}


