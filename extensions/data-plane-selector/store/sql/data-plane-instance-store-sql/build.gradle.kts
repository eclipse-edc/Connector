plugins {
    `java-library`
    `maven-publish`
}


dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))

    implementation(project(":spi:core-spi"))
    implementation(project(":core:common:lib:core-lib"))
    implementation(project(":extensions:common:sql:sql-lease"))
    implementation(project(":extensions:common:sql:sql-bootstrapper"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":spi:control-plane-spi")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))

}


