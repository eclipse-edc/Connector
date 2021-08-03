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
        create<MavenPublication>("schema.aws.s3") {
            artifactId = "dataspaceconnector.schema.aws.s3"
        }
    }
}
