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
 *       Fraunhofer Institute for Software and Systems Engineering - update dependencies
 *
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":core:common:util"))
    api(project(":data-protocols:ids:ids-spi"))

    api(libs.fraunhofer.infomodel)

    implementation(libs.jakarta.rsApi)
    implementation(libs.okhttp)
    implementation(project(":data-protocols:ids:ids-jsonld-serdes"))
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
