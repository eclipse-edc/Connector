/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(project(":spi:control-plane-spi"))
    api(project(":spi:core-spi"))

    implementation(project(":core:common:lib:core-lib"))
    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:junit-base"))
    testImplementation(project(":core:control-plane:control-plane-transfer"))
    testImplementation(libs.awaitility)
}
