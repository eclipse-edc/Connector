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
}

dependencies {
    api(project(":spi:common:keys-spi"))
    api(libs.nimbus.jwt) // nimbus classes are exposed on the API surface of CryptoConverter and DefaultJwsSignerProvider
    implementation(project(":spi:common:boot-spi"))
    implementation(project(":spi:common:jwt-signer-spi"))

    // used for the Ed25519 Verifier in conjunction with OctetKeyPairs (OKP)
    runtimeOnly(libs.tink)
    // Java does not natively implement elliptic curve multiplication, so we need to get bouncy
    implementation(libs.bouncyCastle.bcprovJdk18on)
    testImplementation(testFixtures(project(":core:common:junit")))
    testRuntimeOnly(libs.tink)

}
