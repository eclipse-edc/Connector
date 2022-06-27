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

val openTelemetryVersion: String by project

dependencies {
    api(project(":spi"))

    implementation(project(":core:transfer"))
    implementation(project(":extensions:dataloading"))
    implementation(project(":extensions:data-plane-transfer:data-plane-transfer-client"))
    implementation(project(":extensions:data-plane-selector:selector-client"))
    implementation(project(":extensions:data-plane-selector:selector-core"))
    implementation(project(":extensions:data-plane-selector:selector-store"))
    implementation(project(":extensions:data-plane:data-plane-framework"))
    implementation(project(":extensions:data-plane:data-plane-spi"))

    implementation(project(":extensions:aws:data-plane-s3"))
    implementation(project(":extensions:azure:data-plane:storage"))

    implementation("io.opentelemetry:opentelemetry-extension-annotations:${openTelemetryVersion}")
}