plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:decentralized-claims-spi"))
    api(project(":spi:common:policy:request-policy-context-spi"))
    implementation(project(":spi:common:json-ld-spi"))
    implementation(project(":spi:common:http-spi"))
    implementation(project(":spi:common:keys-spi"))
    implementation(project(":spi:common:participant-spi"))
    implementation(project(":spi:common:participant-context-config-spi"))
    implementation(project(":spi:common:protocol-spi"))
    implementation(project(":core:common:lib:util-lib"))
    implementation(project(":core:common:lib:crypto-common-lib"))
    implementation(project(":core:common:lib:token-lib"))
    implementation(project(":extensions:common:crypto:lib:jws2020-lib"))
    implementation(project(":extensions:common:crypto:jwt-verifiable-credentials"))
    implementation(project(":extensions:common:crypto:ldp-verifiable-credentials"))
    implementation(project(":extensions:common:iam:decentralized-claims:decentralized-claims-service"))
    implementation(project(":extensions:common:iam:decentralized-claims:decentralized-claims-transform"))
    implementation(project(":extensions:common:iam:decentralized-claims:decentralized-claims-sts:decentralized-claims-sts-remote-client"))
    implementation(project(":extensions:common:iam:decentralized-claims:lib:decentralized-claims-lib"))
    implementation(project(":extensions:common:iam:verifiable-credentials"))
    implementation(libs.nimbus.jwt)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":spi:common:decentralized-claims-spi")))
    testImplementation(testFixtures(project(":spi:common:verifiable-credentials-spi")))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.awaitility)
}

