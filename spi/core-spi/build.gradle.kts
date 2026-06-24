/*
 *  Copyright (c) 2021 Microsoft Corporation
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
    `maven-publish`
    `java-test-fixtures`
}

dependencies {
    api(libs.edc.runtime.metamodel)
    api(libs.failsafe.core)
    api(libs.bundles.jackson)
    api(libs.jackson.annotations)
    api(libs.jackson.databind)
    api(libs.jetbrains.annotations)
    api(libs.okhttp)
    api(libs.failsafe.okhttp)
    api(libs.jakarta.rsApi)
    api(libs.jakarta.servlet.api)
    api(libs.jakarta.json.api)
    api(libs.nimbus.jwt)
    api(libs.iron.vc) {
        //this is not on MavenCentral, and we don't really need it anyway
        exclude("com.github.multiformats")
    }

    implementation(libs.opentelemetry.api)
    implementation(libs.parsson)
    implementation(libs.jakarta.annotation)

    testImplementation(project(":core:common:junit-base"))
    testImplementation(project(":core:common:lib:json-lib"))

    testFixturesImplementation(project(":core:common:junit"))
    testFixturesImplementation(project(":core:common:junit-base"))
    testFixturesImplementation(libs.nimbus.jwt)
}


