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
    api(project(":spi:control-plane:asset-spi"))
    api(project(":spi:control-plane:control-plane-spi"))

    implementation(project(":core:common:lib:store-lib"))
    implementation(project(":core:common:boot"))
    implementation(project(":core:control-plane:control-plane-catalog"))
    implementation(project(":core:control-plane:control-plane-contract"))
    implementation(project(":core:control-plane:control-plane-contract-manager"))
    implementation(project(":core:control-plane:control-plane-transfer"))
    implementation(project(":core:control-plane:control-plane-aggregate-services"))
    implementation(project(":core:common:lib:util-lib"))
    implementation(project(":core:common:lib:policy-engine-lib"))
    implementation(project(":core:common:lib:query-lib"))

    testImplementation(testFixtures(project(":spi:control-plane:asset-spi")))
    testImplementation(testFixtures(project(":spi:control-plane:contract-spi")))
    testImplementation(testFixtures(project(":spi:control-plane:policy-spi")))
    testImplementation(testFixtures(project(":spi:control-plane:transfer-spi")))
}


