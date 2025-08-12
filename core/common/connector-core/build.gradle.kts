/*
 *  Copyright (c) 2021 - 2022 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:participant-spi"))

    implementation(project(":core:common:lib:http-lib"))
    implementation(project(":core:common:lib:keys-lib"))
    implementation(project(":core:common:lib:policy-engine-lib"))

    implementation(libs.bouncyCastle.bcpkixJdk18on)
    implementation(libs.nimbus.jwt)
    implementation(libs.tink)

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
}


