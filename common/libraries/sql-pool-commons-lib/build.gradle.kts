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
 *       Daimler TSS GmbH - Initial build file
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

val apacheCommonsPool2Version: String by project

dependencies {
    api(project(":common:libraries:sql-lib"))

    implementation("org.apache.commons:commons-pool2:${apacheCommonsPool2Version}")
}

publishing {
    publications {
        create<MavenPublication>("common-sql-pool-commons-lib") {
            artifactId = "common-sql-pool-commons-lib"
            from(components["java"])
        }
    }
}
