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
    api(project(":spi:common:boot-spi"))
    api(project(":spi:common:policy-model"))

    testImplementation(project(":core:common:junit-base"))
    testImplementation(project(":core:common:lib:json-lib"))
}


