plugins {
    `java-library`
}


dependencies {
    api(project(":spi:common:identity-did-spi"))
    implementation(project(":core:common:util"))

    testImplementation(project(":core:common:junit"))
}


