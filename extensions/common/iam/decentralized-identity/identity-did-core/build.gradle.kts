plugins {
    `java-library`
}


dependencies {
    api(project(":spi:common:identity-did-spi"))
    implementation(project(":core:common:util"))

    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:common:junit"))
}


