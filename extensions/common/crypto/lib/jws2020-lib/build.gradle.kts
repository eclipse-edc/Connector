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
    implementation(project(":spi:common:jwt-spi"))
    implementation(project(":spi:common:json-ld-spi"))
    implementation(project(":extensions:common:json-ld"))
    implementation(project(":core:common:lib:util-lib"))
    implementation(project(":core:common:lib:crypto-common-lib"))
    implementation(libs.nimbus.jwt)
    // used for the Ed25519 Verifier in conjunction with OctetKeyPairs (OKP)
    runtimeOnly(libs.tink)
    implementation(libs.jakarta.json.api)

    api(libs.iron.vc) {
        exclude("com.github.multiformats")
    }

    testImplementation(testFixtures(project(":core:common:junit")))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testFixturesImplementation(testFixtures(project(":core:common:junit")))
    testFixturesImplementation(libs.nimbus.jwt)
    testFixturesImplementation(project(":core:common:lib:json-ld-lib"))
}
