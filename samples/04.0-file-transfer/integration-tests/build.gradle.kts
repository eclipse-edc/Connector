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
    `java-test-fixtures`
}

val restAssured: String by project
val awaitility: String by project
val assertj: String by project
val jupiterVersion: String by project


dependencies {
    testImplementation(project(":core:common:junit"))

    testFixturesImplementation(project(":spi:control-plane:control-plane-spi"))
    testFixturesImplementation(project(":core:common:junit"))
    testFixturesImplementation(project(":extensions:control-plane:api:data-management-api:transfer-process-api"))
    testFixturesImplementation(project(":extensions:common:api:api-core"))
    testFixturesImplementation("io.rest-assured:rest-assured:${restAssured}")
    testFixturesImplementation("org.awaitility:awaitility:${awaitility}")
    testFixturesImplementation("org.assertj:assertj-core:${assertj}")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")

    testCompileOnly(project(":samples:04.0-file-transfer:consumer"))
    testCompileOnly(project(":samples:04.0-file-transfer:provider"))
}
