/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val awsVersion: String by project

dependencies {
    api(project(":spi"))
    implementation(project(":common:util"))

    implementation(project(":extensions:transfer:transfer-provision-aws"))

    implementation("software.amazon.awssdk:s3:${awsVersion}")
    implementation("software.amazon.awssdk:sts:${awsVersion}")
    implementation("software.amazon.awssdk:iam:${awsVersion}")
}
