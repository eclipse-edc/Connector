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
 *       Microsoft Corporation - initial test implementation for sample
 *
 */

plugins {
    `java-library`
}

val restAssured: String by project
val awaitility: String by project


dependencies {
    testImplementation(project(":core:common:junit"))
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation(project(":extensions:control-plane:api:data-management-api:transfer-process-api"))
    testImplementation(project(":extensions:common:api:api-core"))

    testImplementation(testFixtures(project(":samples:04.0-file-transfer:integration-tests")))

    testCompileOnly(project(":samples:04.2-modify-transferprocess:consumer"))
}
