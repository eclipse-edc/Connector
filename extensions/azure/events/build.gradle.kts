/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val eventGridSdkVersion: String by project

dependencies {
    api(project(":edc-core:spi"))
    implementation(project(":edc-core:schema"))
    implementation(project(":common:util"))
    implementation("com.azure:azure-messaging-eventgrid:${eventGridSdkVersion}")
}


publishing {
    publications {
        create<MavenPublication>("events-azure") {
            artifactId = "edc.events-azure"
            from(components["java"])
        }
    }
}