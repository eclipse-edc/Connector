/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       2026 Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:core-spi"))
    implementation(libs.nats)
    implementation(libs.cloudEvents)
    implementation(libs.cloudEvents.json)
    implementation(libs.failsafe.core)

    testImplementation(testFixtures(project(":core:common:junit")))
    testImplementation(project(":core:common:connector-core"))
    testImplementation(project(":core:common:runtime-core"))
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.awaitility)
}


