/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */


plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:auth-spi"))
    api(project(":spi:common:core-spi"))
    implementation(libs.jakarta.rsApi)
}

publishing {
    publications {
        create<MavenPublication>("auth-basic") {
            artifactId = "auth-basic"
            from(components["java"])
        }
    }
}
