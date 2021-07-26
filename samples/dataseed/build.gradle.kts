/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":edc:spi"))
    api(project(":samples:dataseed:dataseed-atlas"))
    api(project(":samples:dataseed:dataseed-policy"))
    api(project(":samples:dataseed:dataseed-nifi"))
    api(project(":samples:dataseed:dataseed-azure"))
    api(project(":samples:dataseed:dataseed-aws"))
}


