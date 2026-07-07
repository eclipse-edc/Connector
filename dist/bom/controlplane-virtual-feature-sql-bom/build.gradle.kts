/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":dist:bom:controlplane-feature-sql-bom")) {
        exclude("org.eclipse.edc", "edr-index-sql")
    }
    api(project(":extensions:control-plane:store:sql:participantcontext-store-sql"))
    api(project(":extensions:control-plane:store:sql:participantcontext-config-store-sql"))
    api(project(":extensions:control-plane:store:sql:dataspace-profile-store-sql"))
    api(project(":extensions:common:store:sql:cel-store-sql"))
    api(project(":extensions:common:store:sql:json-ld-cache-store-sql"))
    api(project(":extensions:common:store:sql:task-store-sql"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-store-sql"))
}

edcBuild {

}
