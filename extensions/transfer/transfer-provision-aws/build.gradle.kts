/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val awsVersion: String by project
val jodahFailsafeVersion: String by project

dependencies {
    api(project(":spi"))

    api(project(":extensions:schema"))

    implementation("software.amazon.awssdk:s3:${awsVersion}")
    implementation("software.amazon.awssdk:sts:${awsVersion}")
    implementation("software.amazon.awssdk:iam:${awsVersion}")
    implementation("net.jodah:failsafe:${jodahFailsafeVersion}")
}




