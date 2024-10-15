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
    implementation(project(":spi:common:policy:request-policy-context-spi"))
    implementation(project(":spi:common:transaction-spi"))
    implementation(project(":spi:common:validator-spi"))
    implementation(project(":spi:control-plane:asset-spi"))
    implementation(project(":spi:control-plane:control-plane-spi"))
    implementation(project(":spi:control-plane:secrets-spi"))
    implementation(project(":spi:control-plane:transfer-data-plane-spi"))
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


