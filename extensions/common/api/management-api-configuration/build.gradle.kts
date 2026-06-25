/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation and others
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       ZF Friedrichshafen AG - Remove token-based default auth mechanism
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))

    implementation(project(":core:common:lib:jsonld-lib"))
    implementation(project(":core:control-plane:control-plane-transform"))
    implementation(project(":core:common:lib:core-lib"))

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(project(":extensions:common:json-ld"))
}
