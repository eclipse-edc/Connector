plugins {
    `java-library`
}


dependencies {
    api(project(":spi:common:identity-did-spi"))
    implementation(project(":core:common:util"))

    implementation(libs.bouncyCastle.bcpkixJdk18on)

    testImplementation(project(":core:common:junit"))
}


