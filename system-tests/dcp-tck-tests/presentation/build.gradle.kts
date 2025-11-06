/*
 *  Copyright (c) 2025 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

plugins {
    java
}

dependencies {
    testRuntimeOnly(libs.dcp.testcases)
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":spi:common:identity-trust-spi"))
    testImplementation(project(":spi:common:identity-did-spi")) //DidDocument
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.restAssured)

    testImplementation(libs.dcp.tck.runtime)
    testImplementation(libs.dcp.system)
    testImplementation(libs.dsp.core)
    testImplementation(libs.wiremock)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.testcontainers.junit)
}

edcBuild {
    publish.set(false)
}
