/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */
plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":extensions:common:crypto:lib:jws2020-lib"))
    implementation(project(":spi:common:json-ld-spi"))
    implementation(project(":spi:common:decentralized-claims-spi"))
    implementation(project(":core:common:lib:util-lib")) //exposes SignatureSuite in its public API

    api(project(":spi:common:identity-did-spi"))
    // used for the Ed25519 Verifier in conjunction with OctetKeyPairs (OKP)
    runtimeOnly(libs.tink)

    api(libs.iron.vc) {
        exclude("com.github.multiformats")
    }

    testImplementation(testFixtures(project(":core:common:junit")))
    testImplementation(project(":extensions:common:crypto:lib:jws2020-lib"))
    testImplementation(project(":core:common:lib:json-ld-lib"))

    // deps for test fixtures
    testFixturesImplementation(project(":spi:common:json-ld-spi"))
    testFixturesImplementation(project(":core:common:lib:json-ld-lib"))
    testFixturesImplementation(project(":extensions:common:crypto:lib:jws2020-lib"))
    testFixturesApi(libs.nimbus.jwt)
    testFixturesApi(testFixtures(project(":extensions:common:crypto:lib:jws2020-lib")))


}
