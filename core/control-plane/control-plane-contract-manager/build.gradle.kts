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
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))
    api(project(":spi:dataspace-protocol-spi"))

    implementation(project(":core:control-plane:lib:control-plane-lib"))
    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(project(":spi:core-spi"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":core:control-plane:control-plane-contract"))
    testImplementation(project(":core:control-plane:control-plane-aggregate-services"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:core-lib"))
    testImplementation(project(":core:control-plane:lib:control-plane-lib"))

    testImplementation(libs.awaitility)
}


