/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.api.management.schema;

public interface ManagementApiJsonSchema {

    String EDC_MGMT_V4_SCHEMA_PREFIX = "https://w3id.org/edc/connector/management/schema/v4";
    String DSPACE_2025_SCHEMA_PREFIX = "https://w3id.org/dspace/2025/1";

    interface V4 {
        String ID_RESPONSE = EDC_MGMT_V4_SCHEMA_PREFIX + "/id-response-schema.json";
        String ASSET = EDC_MGMT_V4_SCHEMA_PREFIX + "/asset-schema.json";
        String CATALOG_ASSET = EDC_MGMT_V4_SCHEMA_PREFIX + "/catalog-asset-schema.json";
        String QUERY_SPEC = EDC_MGMT_V4_SCHEMA_PREFIX + "/query-spec-schema.json";
        String POLICY_DEFINITION = EDC_MGMT_V4_SCHEMA_PREFIX + "/policy-definition-schema.json";
        String CONTRACT_DEFINITION = EDC_MGMT_V4_SCHEMA_PREFIX + "/contract-definition-schema.json";
        String DATAPLANE_INSTANCE = EDC_MGMT_V4_SCHEMA_PREFIX + "/dataplane-instance-schema.json";
        String EDR_ENTRY = EDC_MGMT_V4_SCHEMA_PREFIX + "/edr-entry-schema.json";
        String POLICY_EVALUATION_REQUEST = EDC_MGMT_V4_SCHEMA_PREFIX + "/policy-evaluation-plan-request-schema.json";
        String POLICY_EVALUATION_PLAN = EDC_MGMT_V4_SCHEMA_PREFIX + "/policy-evaluation-plan-schema.json";
        String POLICY_VALIDATION_RESULT = EDC_MGMT_V4_SCHEMA_PREFIX + "/policy-validation-result-schema.json";
        String SECRET = EDC_MGMT_V4_SCHEMA_PREFIX + "/secret-schema.json";
        String CATALOG_REQUEST = EDC_MGMT_V4_SCHEMA_PREFIX + "/catalog-request-schema.json";
        String DATASET_REQUEST = EDC_MGMT_V4_SCHEMA_PREFIX + "/dataset-request-schema.json";
        String CONTRACT_REQUEST = EDC_MGMT_V4_SCHEMA_PREFIX + "/contract-request-schema.json";
        String CONTRACT_NEGOTIATION = EDC_MGMT_V4_SCHEMA_PREFIX + "/contract-negotiation-schema.json";
        String CONTRACT_NEGOTIATION_STATE = EDC_MGMT_V4_SCHEMA_PREFIX + "/contract-negotiation-schema.json#/definitions/NegotiationState";
        String TERMINATE_NEGOTIATION = EDC_MGMT_V4_SCHEMA_PREFIX + "/contract-terminate-schema.json";
        String CONTRACT_AGREEMENT = EDC_MGMT_V4_SCHEMA_PREFIX + "/contract-agreement-schema.json";
        String TRANSFER_REQUEST = EDC_MGMT_V4_SCHEMA_PREFIX + "/transfer-request-schema.json";
        String TRANSFER_PROCESS = EDC_MGMT_V4_SCHEMA_PREFIX + "/transfer-process-schema.json";
        String TRANSFER_PROCESS_STATE = EDC_MGMT_V4_SCHEMA_PREFIX + "/transfer-process-schema.json#/definitions/TransferState";
        String TERMINATE_TRANSFER = EDC_MGMT_V4_SCHEMA_PREFIX + "/transfer-terminate-schema.json";
        String SUSPEND_TRANSFER = EDC_MGMT_V4_SCHEMA_PREFIX + "/transfer-suspend-schema.json";

    }
}
