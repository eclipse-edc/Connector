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

    constraints {
        // netty's mockserver depends on an older version of the json schema validator, but TCK needs this:
        implementation("com.networknt:json-schema-validator:1.5.8") {
            because("This version is required by the TCK")
        }
    }

    testRuntimeOnly(libs.dcp.testcases)
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":spi:common:identity-trust-spi"))
    testImplementation(project(":spi:common:identity-did-spi")) //DidDocument
    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.restAssured)
    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter.api)

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
