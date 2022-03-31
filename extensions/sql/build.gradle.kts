/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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
    `maven-publish`
}

dependencies {
    implementation(project(":extensions:sql:contract-definition"))
    implementation(project(":extensions:sql:contract-negotiation-store"))
    implementation(project(":extensions:sql:lease"))
    implementation(project(":extensions:sql:pool"))
    implementation(project(":extensions:sql:transfer-process-store"))
}

publishing {
    publications {
        create<MavenPublication>("sql") {
            artifactId = "sql"
            from(components["java"])
        }
    }
}
