/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

val jacksonVersion: String by project
val okHttpVersion: String by project
val jodahFailsafeVersion: String by project


plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
    api("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")
    api("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")
    api("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    api("net.jodah:failsafe:${jodahFailsafeVersion}")

    api(project(":core:policy:policy-model"))

    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("spi") {
            artifactId = "spi"
            from(components["java"])
        }
    }
}
