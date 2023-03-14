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
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":data-protocols:ids:ids-spi"))
    implementation(project(":core:common:util"))
    api(project(":data-protocols:ids:ids-core"))

    api(libs.fraunhofer.infomodel)

    implementation(libs.jakarta.rsApi)
    testImplementation(project(":core:common:junit"))

}


