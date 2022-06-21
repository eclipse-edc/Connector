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
    implementation(project(":extensions:sql:asset-index-sql"))
    implementation(project(":extensions:sql:common-sql"))
    implementation(project(":extensions:sql:contract-definition-store-sql"))
    implementation(project(":extensions:sql:contract-negotiation-store-sql"))
    implementation(project(":extensions:sql:lease-sql"))
    implementation(project(":extensions:sql:policy-store-sql"))
    implementation(project(":extensions:sql:transfer-process-store-sql"))
}

publishing {
    publications {
        create<MavenPublication>("sql") {
            artifactId = "sql"
            from(components["java"])
        }
    }
}
