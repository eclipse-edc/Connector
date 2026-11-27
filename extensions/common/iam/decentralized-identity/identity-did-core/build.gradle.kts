plugins {
    `java-library`
}


dependencies {
    api(project(":spi:core-spi"))
    implementation(project(":spi:core-spi"))
    implementation(project(":core:common:lib:core-lib"))

    implementation(libs.bouncyCastle.bcpkixJdk18on)

    testImplementation(libs.edc.junit.base);
    testImplementation(project(":core:common:junit"))

}


