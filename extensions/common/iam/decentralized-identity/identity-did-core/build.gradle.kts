plugins {
    `java-library`
}


dependencies {
    api(project(":spi:common:identity-did-spi"))
    implementation(project(":spi:common:keys-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":core:common:lib:keys-lib"))

    implementation(libs.bouncyCastle.bcpkixJdk18on)

    testImplementation(project(":core:common:junit"))
}


