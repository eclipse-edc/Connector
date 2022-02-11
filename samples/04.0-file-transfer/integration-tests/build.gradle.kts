/*
 *  Copyright (c) 2022 Microsoft Corporation
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
    java
}

val jupiterVersion: String by project

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("io.rest-assured:rest-assured:4.5.0")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("org.awaitility:awaitility:4.1.1")

    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))

    testRuntimeOnly(project(":samples:04.0-file-transfer:provider"))
    testRuntimeOnly(project(":samples:04.0-file-transfer:consumer"))
}

tasks.getByName<Test>("test") {
    testLogging {
        showStandardStreams = true
    }
}
