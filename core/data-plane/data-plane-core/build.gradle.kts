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
    api(project(":spi:core-spi"))
    api(project(":spi:data-plane-spi"))

    implementation(project(":spi:core-spi"))
    implementation(project(":core:control-plane:lib:control-plane-lib"))
    implementation(project(":core:common:lib:core-lib"))
    implementation(project(":core:data-plane:data-plane-util"))

    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(project(":core:common:lib:core-lib"))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
    testImplementation(testFixtures(project(":spi:data-plane-spi")))
}


