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
    `maven-publish`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:token-spi"))
    api(project(":spi:common:auth-spi"))
    api(project(":spi:common:connector-participant-context-spi"))


    implementation(project(":spi:common:web-spi"))
    implementation(project(":core:common:lib:util-lib"))


    implementation(libs.jakarta.rsApi)
    implementation(libs.jakarta.annotation)
    implementation(libs.nimbus.jwt)

    testImplementation(project(":core:common:junit"))
    testRuntimeOnly(libs.jersey.common) // needs the RuntimeDelegate
}
