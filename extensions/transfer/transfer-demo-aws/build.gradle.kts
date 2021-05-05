/*
* Copyright (c) Microsoft Corporation.
* All rights reserved.
*/

val awsVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    implementation(project(":extensions:schema"))
    implementation(project(":extensions:transfer:transfer-provision-aws"))

    implementation(platform("software.amazon.awssdk:bom:${awsVersion}"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:sts")
    implementation("software.amazon.awssdk:auth")
}


