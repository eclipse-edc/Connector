plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:identity-did-spi"))
    api(project(":spi:common:jwt-spi"))

    implementation(libs.bouncyCastle.bcpkixJdk18on)
    testImplementation(project(":core:common:junit"))
}


