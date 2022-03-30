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
    implementation(project(":extensions:api:api-core"))
    api(project(":extensions:api:auth-spi"))
}

publishing {
    publications {
        create<MavenPublication>("api-configuration") {
            artifactId = "api-configuration"
            from(components["java"])
        }
    }
}
