/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    `maven-publish`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:token-spi"))
    api(libs.nimbus.jwt)
    runtimeOnly(libs.tink) // for EdDSA/Ed25519
    implementation(libs.bouncyCastle.bcprovJdk18on)
    // Java does not natively implement elliptic curve multiplication, so we need to get bouncy
    implementation(libs.bouncyCastle.bcpkixJdk18on)

    testImplementation(libs.bouncyCastle.bcprovJdk18on) // for EdDSA/Ed25519

}


