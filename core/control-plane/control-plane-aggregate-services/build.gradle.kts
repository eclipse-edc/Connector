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
    api(project(":spi:common:boot-spi"))
    api(project(":spi:common:policy:request-policy-context-spi"))
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:common:validator-spi"))
    api(project(":spi:control-plane:asset-spi"))
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:control-plane:secrets-spi"))
    api(project(":spi:control-plane:transfer-data-plane-spi"))
    implementation(project(":core:common:lib:util-lib"))

    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(project(":core:common:connector-core"))
    testImplementation(project(":core:control-plane:control-plane-catalog"))
    testImplementation(project(":core:control-plane:control-plane-contract"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":core:data-plane-selector:data-plane-selector-core"))
    testImplementation(project(":core:control-plane:control-plane-transfer"))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.awaitility)
}


