/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    api(project(":common:util"))
    api(project(":extensions:azure:blob:blob-schema"))
    api(project(":extensions:aws:s3:s3-schema"))

}

publishing {
    publications {
        create<MavenPublication>("atlas.catalog") {
            artifactId = "edc.atlas.catalog"
            from(components["java"])
        }
    }
}