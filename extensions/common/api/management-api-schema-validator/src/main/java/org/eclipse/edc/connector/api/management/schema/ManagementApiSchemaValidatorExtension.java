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

package org.eclipse.edc.connector.api.management.schema;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.DSPACE_2025_SCHEMA_PREFIX;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.EDC_MGMT_V4_SCHEMA_PREFIX;
import static org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension.NAME;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand.TERMINATE_NEGOTIATION_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE_TERM;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TYPE_TERM;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_TYPE_TERM;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE_TERM;

@Extension(NAME)
public class ManagementApiSchemaValidatorExtension implements ServiceExtension {

    public static final String NAME = "Management API Schema Validator";
    public static final String V_4 = "v4";
    public static final String V_4_PREFIX = V_4 + ":";
    private static final String EDC_CLASSPATH_SCHEMA = "classpath:schema/management/v4";
    private static final String DSPACE_CLASSPATH_SCHEMA = "classpath:schema/dspace/2025";

    private final Map<String, String> schemaV4 = new HashMap<>() {
        {
            put(DATASET_REQUEST_TYPE_TERM, "dataset-request-schema.json");
            put(CATALOG_REQUEST_TYPE_TERM, "catalog-request-schema.json");
            put(EDC_QUERY_SPEC_TYPE_TERM, "query-spec-schema.json");
            put(EDC_ASSET_TYPE_TERM, "asset-schema.json");
            put(CONTRACT_DEFINITION_TYPE_TERM, "contract-definition-schema.json");
            put(CONTRACT_AGREEMENT_TYPE_TERM, "contract-agreement-schema.json");
            put(EDR_ENTRY_TYPE_TERM, "edr-entry-schema.json");
            put(EDC_POLICY_DEFINITION_TYPE_TERM, "policy-definition-schema.json");
            put("PolicyEvaluationPlanRequest", "policy-evaluation-plan-request-schema.json");
            put(EDC_POLICY_EVALUATION_PLAN_TYPE_TERM, "policy-evaluation-plan-schema.json");
            put("PolicyValidationResult", "policy-validation-result-schema.json");
            put(EDC_SECRET_TYPE_TERM, "secret-schema.json");
            put(CONTRACT_REQUEST_TYPE_TERM, "contract-request-schema.json");
            put(TERMINATE_NEGOTIATION_TYPE_TERM, "contract-terminate-schema.json");
            put(CONTRACT_NEGOTIATION_TYPE_TERM, "contract-negotiation-schema.json");
            put("NegotiationState", "contract-negotiation-schema.json#/definitions/NegotiationState");
            put(TRANSFER_REQUEST_TYPE_TERM, "transfer-request-schema.json");
            put(TRANSFER_PROCESS_TYPE_TERM, "transfer-process-schema.json");
            put("TransferState", "transfer-process-schema.json#/definitions/TransferState");
            put("TerminateTransfer", "transfer-terminate-schema.json");
            put("SuspendTransfer", "transfer-suspend-schema.json");
        }
    };

    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonObjectValidatorRegistry validator;

    @Override
    public void initialize(ServiceExtensionContext context) {

        var schemaValidatorProvider = ManagementApiSchemaValidatorProvider.Builder.newInstance()
                .objectMapper(() -> typeManager.getMapper(JSON_LD))
                .prefixMapping(EDC_MGMT_V4_SCHEMA_PREFIX, EDC_CLASSPATH_SCHEMA)
                .prefixMapping(DSPACE_2025_SCHEMA_PREFIX, DSPACE_CLASSPATH_SCHEMA)
                .build();

        registerValidatorsV4(schemaValidatorProvider);
    }

    void registerValidatorsV4(ManagementApiSchemaValidatorProvider validatorProvider) {
        schemaV4.forEach((type, schema) -> validator.register(V_4_PREFIX + type, validatorProvider.validatorFor(EDC_MGMT_V4_SCHEMA_PREFIX + "/" + schema)));
    }

}
