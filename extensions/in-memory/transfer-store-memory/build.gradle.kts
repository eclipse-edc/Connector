/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))


}
publishing {
    publications {
        create<MavenPublication>("in-mem.process-store") {
            artifactId = "in-mem.process-store"
            from(components["java"])
        }
    }
}
