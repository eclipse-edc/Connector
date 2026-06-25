/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))
    api(project(":spi:decentralized-claims-spi"))
    api(libs.nimbus.jwt) // nimbus classes are exposed on the API surface of CryptoConverter and DefaultJwsSignerProvider
    api(libs.bouncyCastle.bcpkixJdk18on)

    implementation(project(":core:common:lib:jsonld-lib"))
    // Java does not natively implement elliptic curve multiplication, so we need to get bouncy
    implementation(libs.bouncyCastle.bcprovJdk18on)
    // used for the Ed25519 Verifier in conjunction with OctetKeyPairs (OKP)
    implementation(libs.tink)
    implementation(libs.jakarta.rsApi)
    implementation(libs.jakarta.annotation)
    implementation(libs.jsonschema)
    implementation(libs.okhttp)
    implementation(libs.dnsOverHttps)
    implementation(libs.nats.client)
    implementation(libs.swagger.annotations.jakarta)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":core:common:junit")))
    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(testFixtures(project(":core:common:lib:jsonld-lib")))
    testImplementation(testFixtures(project(":spi:core-spi")))
    testImplementation(testFixtures(project(":spi:decentralized-claims-spi")))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    testImplementation(libs.awaitility)
    testImplementation(libs.wiremock)
    testImplementation(libs.restAssured)
    testImplementation(libs.junit.pioneer)

    testRuntimeOnly(libs.jersey.common) // needs the RuntimeDelegate

    testFixturesImplementation(libs.wiremock) {
        exclude("com.networknt", "json-schema-validator")
    }
    testFixturesImplementation(libs.jakarta.json.api)
    testFixturesImplementation(libs.nimbus.jwt)
    testFixturesImplementation(libs.nats.client)
    testFixturesImplementation(libs.testcontainers.junit)
}
