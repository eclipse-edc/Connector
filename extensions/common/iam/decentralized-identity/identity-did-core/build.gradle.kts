plugins {
    `java-library`
}


dependencies {
    api(project(":spi:common:identity-did-spi"))
    implementation(project(":spi:common:keys-spi"))
    implementation(project(":core:common:lib:util-lib"))
    implementation(project(":core:common:lib:keys-lib"))

    implementation(libs.bouncyCastle.bcpkixJdk18on)

    testImplementation(project(":core:common:junit-base"));

}


