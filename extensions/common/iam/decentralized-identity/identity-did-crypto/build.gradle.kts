plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:identity-did-spi"))
    api(project(":spi:common:jwt-spi"))

    implementation(libs.bouncyCastle.bcpkixJdk18on)
    implementation(project(":core:common:token-core")) // this will go away once JwtUtils is deleted
    testImplementation(project(":core:common:junit"))
}


