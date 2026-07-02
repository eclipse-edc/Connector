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
}


dependencies {
    api(project(":spi:control-plane-spi"))
    api(project(":spi:core-spi"))

    implementation(project(":core:control-plane:lib:control-plane-lib"))
    implementation(project(":core:common:boot"))
    implementation(project(":core:common:participant-context-core"))
    implementation(project(":core:common:participant-context-connector-core"))
    implementation(project(":core:common:participant-context-config-core"))
    implementation(project(":core:control-plane:control-plane-catalog"))
    implementation(project(":core:control-plane:control-plane-contract"))
    implementation(project(":core:control-plane:control-plane-transfer"))
    implementation(project(":core:control-plane:control-plane-aggregate-services"))
    implementation(project(":core:common:lib:core-lib"))

    testImplementation(testFixtures(project(":spi:control-plane-spi")))
    testImplementation(testFixtures(project(":spi:dataspace-protocol-spi")))
}


