/*
 *  Copyright (c) 2021 Copyright Holder (Catena-X Consortium)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      Catena-X Consortium - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}


dependencies {
    api(project(":spi"))

    testImplementation("com.github.javafaker:javafaker:1.0.2")
    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
}

publishing {
    publications {
        create<MavenPublication>("jdk-logger-monitor") {
            groupId = "org.eclipse.dataspaceconnector.logger"
            artifactId = "jdk-logger-monitor"
            version = "0.1"
            from(components["java"])
        }
    }
}
