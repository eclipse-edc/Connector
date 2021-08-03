/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}

val cosmosSdkVersion: String by project

dependencies {
    api(project(":spi"))
    api(project(":common:util"))
    implementation("com.azure:azure-cosmos:${cosmosSdkVersion}")

    testImplementation(testFixtures(project(":common:util")))
}


publishing {
    publications {
        create<MavenPublication>("azure.cosmos.processstore") {
            artifactId = "dataspaceconnector.transfer-process-store.cosmos"
            from(components["java"])
        }
    }
}
