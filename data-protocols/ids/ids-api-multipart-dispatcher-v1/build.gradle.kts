/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":data-protocols:ids:ids-spi"))
    api(project(":data-protocols:ids:ids-core"))
    api(project(":data-protocols:ids:ids-transform-v1"))
    implementation(project(":data-protocols:ids:ids-api-configuration"))

    implementation(libs.jakarta.rsApi)
    implementation(libs.jersey.multipart)
    implementation(libs.okhttp)

    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":data-protocols:ids:ids-api-multipart-endpoint-v1"))
    testImplementation(project(":core:common:junit"))
}

publishing {
    publications {
        create<MavenPublication>("ids-api-multipart-dispatcher-v1") {
            artifactId = "ids-api-multipart-dispatcher-v1"
            from(components["java"])
        }
    }
}
