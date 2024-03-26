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
    api(project(":spi:policy-monitor:policy-monitor-spi"))
    api(project(":spi:common:json-ld-spi"))
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:control-plane:policy-spi"))
    api(project(":spi:control-plane:transfer-spi"))

    implementation(project(":core:common:lib:state-machine-lib"))
    implementation(project(":core:common:lib:store-lib"))
    implementation(project(":core:control-plane:lib:control-plane-policies-lib"))

    testImplementation(project(":core:common:lib:query-lib"))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
    testImplementation(testFixtures(project(":spi:policy-monitor:policy-monitor-spi")))
}


