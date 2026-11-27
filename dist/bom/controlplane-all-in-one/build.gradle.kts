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
    // The virtual control plane with all major features on the classpath. Which of them are loaded is selected through
    // the "edc.feature.*" configuration, see the "conditional dependency resolution" decision record. Without any
    // feature key, the runtime uses the polling-based state machine, the in-memory stores and the in-memory vault.
    api(project(":dist:bom:controlplane-virtual-base-bom"))
    api(project(":dist:bom:controlplane-virtual-feature-dcp-bom"))

    // edc.feature.statemachine: "polling" (the default) selects the managers, "task" the task-based state machine,
    // which is part of the base BOM and uses NATS for message passing
    api(project(":core:control-plane:control-plane-contract-manager"))
    api(project(":core:control-plane:control-plane-transfer-manager"))
    // also contains the NKey authentication: edc.feature.nats.auth=nkey
    api(project(":dist:bom:controlplane-virtual-feature-nats-bom"))

    // edc.feature.persistence=postgres
    api(project(":dist:bom:controlplane-virtual-feature-sql-bom"))

    // edc.feature.vault=hashicorp
    api(project(":extensions:common:vault:vault-hashicorp"))

    // edc.feature.events=nats
    api(project(":extensions:common:events:events-nats"))

    // edc.feature.observability=true selects the Micrometer metrics. The OpenTelemetry monitor of this BOM is not bound
    // to a feature: it does not emit anything unless an OpenTelemetry SDK or the OpenTelemetry Java agent is present
    api(project(":dist:bom:controlplane-feature-observability-bom"))
}

edcBuild {

}
