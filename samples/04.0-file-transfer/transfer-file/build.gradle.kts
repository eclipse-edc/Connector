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
 *       Fraunhofer Institute for Software and Systems Engineering - added dependencies
 *
 */

plugins {
    `java-library`
    id("application")
}

val openTelemetryVersion: String by project

dependencies {
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:data-plane:data-plane-spi"))
    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":core:data-plane:data-plane-core"))
    implementation(project(":core:data-plane:data-plane-common"))
    implementation(project(":extensions:control-plane:data-plane-transfer:data-plane-transfer-client"))
    implementation(project(":extensions:data-plane-selector:selector-client"))
    implementation(project(":core:data-plane-selector:data-plane-selector-core"))
    implementation("io.opentelemetry:opentelemetry-extension-annotations:${openTelemetryVersion}")

    implementation(project(":spi:data-plane:data-plane-spi"))
}
