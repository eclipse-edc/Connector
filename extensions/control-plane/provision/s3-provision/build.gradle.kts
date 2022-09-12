/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
}

dependencies {
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":extensions:common:aws:s3-core"))

    testImplementation(testFixtures(project(":extensions:common:aws:aws-test")))
}

publishing {
    publications {
        create<MavenPublication>("s3-provision") {
            artifactId = "s3-provision"
            from(components["java"])
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform {
        systemProperty("it.aws.region", System.getProperty("it.aws.region"))
        systemProperty("it.aws.endpoint", System.getProperty("it.aws.endpoint"))
        systemProperty("it.aws.profile", System.getProperty("it.aws.profile"))
    }
}
