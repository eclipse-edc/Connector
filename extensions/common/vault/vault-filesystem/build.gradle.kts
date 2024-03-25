/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
    api(project(":spi:common:core-spi"))
    implementation(project(":spi:common:keys-spi"))
    implementation(project(":core:common:lib:keys-lib"))
    implementation(project(":core:common:util"))
    implementation(libs.bouncyCastle.bcpkixJdk18on)

    testImplementation(libs.nimbus.jwt)
    testImplementation(libs.bouncyCastle.bcprovJdk18on)
    testImplementation(project(":core:common:connector-core"))
}


