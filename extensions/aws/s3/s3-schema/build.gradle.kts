/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val storageBlobVersion: String by project

dependencies {
    api(project(":spi"))
    api(project(":core:schema"))

}

publishing {
    publications {
        create<MavenPublication>("aws.s3.schema") {
            artifactId = "aws.s3.schema"
        }
    }
}
