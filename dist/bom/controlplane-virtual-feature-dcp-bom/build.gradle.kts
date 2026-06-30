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
    api(project(":dist:bom:controlplane-feature-dcp-bom")) {
        exclude("org.eclipse.edc", "decentralized-claims-sts-remote-client")
    }
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-cel"))
    api(project(":extensions:control-plane:api:management-api-v5:dcp-scope-api-v5"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-sts:decentralized-claims-sts-registry"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-sts:decentralized-claims-sts-remote-registrar"))
    api(project(":extensions:common:iam:decentralized-claims:decentralized-claims-sts:decentralized-claims-sts-signature-registrar"))
}

edcBuild {

}
