plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:decentralized-claims-spi"))
    api(project(":spi:control-plane-spi"))
    api(project(":spi:core-spi"))
    implementation(project(":spi:core-spi"))
    implementation(project(":spi:control-plane-spi"))
    implementation(project(":spi:dataspace-protocol-spi"))

    implementation(project(":core:common:lib:core-lib"))
    implementation(project(":core:control-plane:lib:control-plane-lib"))
    implementation(project(":extensions:common:crypto:lib:jws2020-lib"))
    implementation(project(":extensions:common:crypto:jwt-verifiable-credentials"))
    implementation(project(":extensions:common:crypto:ldp-verifiable-credentials"))
    implementation(project(":extensions:common:iam:decentralized-claims:decentralized-claims-service"))
    implementation(project(":extensions:common:iam:decentralized-claims:decentralized-claims-transform"))
    implementation(project(":extensions:common:iam:decentralized-claims:decentralized-claims-sts:decentralized-claims-sts-remote-client"))
    implementation(libs.nimbus.jwt)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":spi:decentralized-claims-spi")))
    testImplementation(testFixtures(project(":spi:core-spi")))
    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(project(":core:common:lib:core-lib"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.awaitility)
}

