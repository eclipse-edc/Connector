/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:transform-spi"))
    api(project(":spi:control-plane:asset-spi"))
    api(project(":spi:control-plane:policy-spi"))
    api(project(":spi:control-plane:transfer-spi"))

    implementation(project(":core:common:lib:transform-lib"))
    implementation(project(":core:control-plane:lib:control-plane-transfer-provision-lib"))
    implementation(libs.opentelemetry.instrumentation.annotations)
    implementation(libs.jakarta.json.api)

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.parsson)
}



