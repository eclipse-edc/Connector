/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val storageBlobVersion: String by project

dependencies {
    api(project(":edc:spi"))
    api(project(":edc:schema"))

}

publishing {
    publications {
        create<MavenPublication>("transfer-provision-azure") {
            artifactId = "edc.transfer-provision-azure"
            from(components["java"])
        }
    }
}