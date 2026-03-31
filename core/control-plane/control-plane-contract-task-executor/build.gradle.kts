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
}

dependencies {
    api(project(":spi:common:task-spi"))
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:control-plane:contract-spi"))
    implementation(libs.opentelemetry.instrumentation.annotations)
    testImplementation(project(":core:control-plane:control-plane-contract"))
    testImplementation(project(":core:common:lib:state-machine-lib"))

}

