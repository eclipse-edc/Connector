/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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
}

dependencies {
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))
    api(project(":spi:common:transaction-spi"))

    implementation(project(":core:common:lib:query-lib"))
    implementation(project(":core:common:lib:state-machine-lib"))
    implementation(project(":core:common:lib:store-lib"))
    implementation(project(":core:common:lib:util-lib"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":spi:data-plane-selector:data-plane-selector-spi")))
    testImplementation(project(":core:common:junit"))
}


