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
    api(project(":spi:common:auth-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:control-plane:transfer-spi"))
    implementation(project(":extensions:common:http:lib:jersey-providers-lib"))

    testImplementation(project(":core:common:junit"))
}


