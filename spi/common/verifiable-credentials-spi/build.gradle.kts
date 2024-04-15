/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
}

dependencies {
    api(project(":spi:common:boot-spi"))
    api(libs.iron.vc) {
        //this is not on MavenCentral, and we don't really need it anyway
        exclude("com.github.multiformats")
    }

    testImplementation(project(":spi:common:core-spi"))
    testImplementation(project(":core:common:lib:json-lib"))

    testFixturesImplementation(libs.nimbus.jwt)
}


