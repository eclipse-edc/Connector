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
} dependencies {
    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":core:data-plane:data-plane-core"))
    implementation(project(":extensions:data-plane:data-plane-azure-storage"))
    implementation(project(":extensions:data-plane:data-plane-aws-s3"))
    implementation(project(":extensions:control-plane:data-plane-transfer:data-plane-transfer-client"))
    implementation(project(":extensions:data-plane-selector:data-plane-selector-client"))
    implementation(project(":core:data-plane-selector:data-plane-selector-core"))

    implementation(libs.opentelemetry.annotations)
}