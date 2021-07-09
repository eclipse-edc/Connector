/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":extensions:transfer:transfer-core"))
    api(project(":extensions:transfer:transfer-demo-protocols"))
    api(project(":extensions:transfer:transfer-provision-aws"))
    api(project(":extensions:transfer:transfer-provision-azure"))
    api(project(":extensions:transfer:transfer-store-cosmos"))
    api(project(":extensions:transfer:transfer-store-memory"))

}

publishing {
    publications {
        create<MavenPublication>("transfer") {
            artifactId = "edc.transfer"
            from(components["java"])
        }
    }
}