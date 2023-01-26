/*
 *  Copyright (c) 2020, 2021, 2022 Amazon Web Services
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amazon Web Services - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))
    implementation("software.amazon.awssdk:secretsmanager:2.19.3")
    implementation(project(":core:common:util"))
    testImplementation(libs.mockito.inline)
}


publishing {
    publications {
        create<MavenPublication>("vault-aws") {
            artifactId = "vault-aws"
            from(components["java"])
        }
    }
}