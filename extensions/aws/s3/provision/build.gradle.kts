/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val awsVersion: String by project

dependencies {
    api(project(":core:spi"))
    api(project(":extensions:aws:s3:s3-schema"))

    implementation("software.amazon.awssdk:s3:${awsVersion}")
    implementation("software.amazon.awssdk:sts:${awsVersion}")
    implementation("software.amazon.awssdk:iam:${awsVersion}")

    testImplementation(testFixtures(project(":common:util")))

}

publishing {
    publications {
        create<MavenPublication>("provision.aws.s3") {
            artifactId = "edc.provision.aws.s3"
            from(components["java"])
        }
    }
}