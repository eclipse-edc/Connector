/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

val infoModelVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    implementation(project(":data-protocols:ids:ids-spi"))

    testImplementation(testFixtures(project(":launchers:junit")))
}


publishing {
    publications {
        create<MavenPublication>("policy-token-claim-validation") {
            artifactId = "policy-token-claim-validation"
            from(components["java"])
        }
    }
}
