/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

plugins {
    `java-library`
}

val googleCloudIamAdminVersion: String by project
val googleCloudIamCredentialsVersion: String by project
val googleCloudStorageVersion: String by project

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:transfer-spi"))

    implementation("com.google.cloud:google-iam-admin:${googleCloudIamAdminVersion}")
    implementation("com.google.cloud:google-cloud-storage:${googleCloudStorageVersion}")
    implementation("com.google.cloud:google-cloud-iamcredentials:${googleCloudIamCredentialsVersion}")
}
publishing {
    publications {
        create<MavenPublication>("gcp-core") {
            artifactId = "gcp-core"
            from(components["java"])
        }
    }
}
