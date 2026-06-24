plugins {
    `java-library`
}


dependencies {
    api(project(":spi:core-spi"))
    implementation(project(":spi:core-spi"))
    implementation(project(":core:common:lib:util-lib"))
    implementation(project(":core:common:lib:keys-lib"))

    implementation(libs.bouncyCastle.bcpkixJdk18on)

    testImplementation(project(":core:common:junit-base"));

}


