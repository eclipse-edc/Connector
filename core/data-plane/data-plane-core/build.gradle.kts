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
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:data-plane:data-plane-spi"))

    implementation(project(":spi:common:token-spi"))
    implementation(project(":core:common:lib:store-lib"))
    implementation(project(":core:common:lib:util-lib"))
    implementation(project(":core:data-plane:data-plane-util"))
    implementation(project(":core:common:lib:state-machine-lib"))

    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(project(":core:common:lib:query-lib"))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
    testImplementation(testFixtures(project(":spi:data-plane:data-plane-spi")))
}


