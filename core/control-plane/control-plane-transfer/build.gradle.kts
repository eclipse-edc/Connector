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
    api(project(":spi:common:protocol-spi"))
    api(project(":spi:common:transform-spi"))
    api(project(":spi:control-plane:asset-spi"))
    api(project(":spi:control-plane:policy-spi"))
    api(project(":spi:control-plane:transfer-spi"))

    implementation(project(":core:common:lib:state-machine-lib"))
    implementation(project(":core:common:lib:util-lib"))

    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(project(":core:common:lib:query-lib"))
    testImplementation(project(":core:common:lib:store-lib"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(libs.awaitility)
}



