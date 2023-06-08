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
    api(project(":spi:common:json-ld-spi"))
    api(project(":spi:common:web-spi"))
    implementation(project(":core:common:jersey-providers"))
    implementation(project(":extensions:common:api:api-core"))

    testImplementation(project(":core:common:junit"))
}

edcBuild {
    swagger {
        apiGroup.set("management-api")
    }
}


