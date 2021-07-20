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
    api(project(":common:util"))
    api(project(":core"))
    implementation(project(":extensions:schema"))

    testImplementation(project(":extensions:catalog:catalog-atlas"))
    testImplementation(project(":extensions:dataseed"))
    testImplementation(project(":extensions:transfer:transfer-provision-azure"))

    // There is an incompatibility between the Amazon SDK, which internally uses httpclient 4.5.4 and
    // the Atlas Client, which is pulled in by the "catalog-atlas" extension, and which uses an earlier version
    // causing all requests to fail with a LambdaConversionException (of all things).
    // Since this situation can only arise in a test scenario, requiring a strict version is acceptable. Another
    // possibility would be to exclude one of the transitive dependencies.
    //
    // more about this problem can be found here:
    // https://github.com/aws/aws-sdk-java-v2/issues/652
    // https://docs.gradle.org/current/userguide/dependency_downgrade_and_exclude.html
    testImplementation("org.apache.httpcomponents:httpclient") {
        version {
            strictly("4.5.5")
        }
    }

    testImplementation("com.azure:azure-storage-blob:12.11.0")
    testImplementation(platform("software.amazon.awssdk:bom:${awsVersion}"))
    testImplementation("software.amazon.awssdk:s3")
}


