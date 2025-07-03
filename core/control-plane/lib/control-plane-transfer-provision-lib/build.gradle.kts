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
    `java-test-fixtures`
}


dependencies {
    api(project(":spi:control-plane:transfer-spi"))

    implementation(project(":core:common:lib:util-lib"))
    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(project(":core:control-plane:control-plane-transfer"))
}


