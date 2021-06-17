/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    api(project(":extensions:dataseed:dataseed-atlas"))
    api(project(":extensions:dataseed:dataseed-policy"))
    api(project(":extensions:dataseed:dataseed-nifi"))
    api(project(":extensions:dataseed:dataseed-azure"))
    api(project(":extensions:dataseed:dataseed-aws"))
}


