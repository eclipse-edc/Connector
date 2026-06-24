plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:decentralized-claims-spi"))
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))
    implementation(project(":extensions:common:iam:decentralized-claims:decentralized-claims-sts:lib:decentralized-claims-sts-remote-lib"))

    testImplementation(project(":core:common:junit"))
}

