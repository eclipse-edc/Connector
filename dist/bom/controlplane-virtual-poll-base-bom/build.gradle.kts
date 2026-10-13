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
    // virtual control plane with the poll-based state machine instead of the task-based one
    api(project(":dist:bom:controlplane-virtual-base-bom")) {
        exclude("org.eclipse.edc", "task-core")
        exclude("org.eclipse.edc", "control-plane-contract-task-executor")
        exclude("org.eclipse.edc", "control-plane-transfer-task-executor")
    }
    api(project(":core:control-plane:control-plane-contract-manager"))
    api(project(":core:control-plane:control-plane-transfer-manager"))
}

edcBuild {

}
