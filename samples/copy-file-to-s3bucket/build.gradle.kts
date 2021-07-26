/*
* Copyright (c) Microsoft Corporation.
* All rights reserved.
*/

val awsVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":edc:spi"))
    implementation(project(":extensions:aws:s3:provision"))
    implementation(project(":extensions:aws:s3:s3-schema"))


    implementation(platform("software.amazon.awssdk:bom:${awsVersion}"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:sts")
    implementation("software.amazon.awssdk:auth")
    implementation("software.amazon.awssdk:iam")
}


