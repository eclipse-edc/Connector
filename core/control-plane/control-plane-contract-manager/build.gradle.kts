/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */
plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:contract-spi"))
    api(project(":spi:common:protocol-spi"))

    implementation(project(":core:common:lib:state-machine-lib"))
    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(project(":spi:common:transaction-spi"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":core:control-plane:control-plane-contract"))
    testImplementation(project(":core:control-plane:control-plane-aggregate-services"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:query-lib"))
    testImplementation(project(":core:common:lib:store-lib"))
//    testImplementation(project(":core:common:lib:policy-engine-lib"))
    testImplementation(libs.awaitility)
}


