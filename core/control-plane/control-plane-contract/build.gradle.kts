/*
*  Copyright (c) 2021 Daimler TSS GmbH
*
*  This program and the accompanying materials are made available under the
*  terms of the Apache License, Version 2.0 which is available at
*  https://www.apache.org/licenses/LICENSE-2.0
*
*  SPDX-License-Identifier: Apache-2.0
*
*  Contributors:
*       Daimler TSS GmbH - Initial API and Implementation
*       Microsoft Corporation - introduced Awaitility
*
*/
plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:json-ld-spi"))
    api(project(":spi:common:policy-engine-spi"))
    api(project(":spi:common:protocol-spi"))
    api(project(":spi:control-plane:asset-spi"))
    api(project(":spi:control-plane:catalog-spi"))
    api(project(":spi:control-plane:contract-spi"))

    implementation(project(":core:common:lib:state-machine-lib"))
    implementation(project(":core:control-plane:lib:control-plane-policies-lib"))
    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(project(":spi:common:transaction-spi"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":core:control-plane:control-plane-aggregate-services"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:query-lib"))
    testImplementation(project(":core:common:lib:store-lib"))
    testImplementation(project(":core:common:lib:policy-engine-lib"))
    testImplementation(libs.awaitility)
}


