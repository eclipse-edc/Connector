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
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.ASSET;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.CATALOG_ASSET;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.CATALOG_REQUEST;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.CONTRACT_AGREEMENT;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.CONTRACT_DEFINITION;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.CONTRACT_NEGOTIATION;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.CONTRACT_NEGOTIATION_STATE;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.CONTRACT_REQUEST;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.DATAPLANE_INSTANCE;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.DATASET_REQUEST;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.EDR_ENTRY;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.ID_RESPONSE;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.POLICY_DEFINITION;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.POLICY_EVALUATION_PLAN;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.POLICY_EVALUATION_REQUEST;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.POLICY_VALIDATION_RESULT;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.QUERY_SPEC;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.SECRET;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.SUSPEND_TRANSFER;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.TERMINATE_NEGOTIATION;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.TERMINATE_TRANSFER;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.TRANSFER_PROCESS;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.TRANSFER_PROCESS_STATE;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4.TRANSFER_REQUEST;
import static org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension.NAME;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_CATALOG_ASSET_TYPE_TERM;
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
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.DATAPLANE_INSTANCE_TYPE_TERM;
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
            put("IdResponse", ID_RESPONSE);
            put(EDC_ASSET_TYPE_TERM, ASSET);
            put(EDC_CATALOG_ASSET_TYPE_TERM, CATALOG_ASSET);
            put(EDC_QUERY_SPEC_TYPE_TERM, QUERY_SPEC);
            put(EDC_POLICY_DEFINITION_TYPE_TERM, POLICY_DEFINITION);
            put(CONTRACT_DEFINITION_TYPE_TERM, CONTRACT_DEFINITION);
            put(DATAPLANE_INSTANCE_TYPE_TERM, DATAPLANE_INSTANCE);
            put(EDR_ENTRY_TYPE_TERM, EDR_ENTRY);
            put("PolicyEvaluationPlanRequest", POLICY_EVALUATION_REQUEST);
            put(EDC_POLICY_EVALUATION_PLAN_TYPE_TERM, POLICY_EVALUATION_PLAN);
            put("PolicyValidationResult", POLICY_VALIDATION_RESULT);
            put(EDC_SECRET_TYPE_TERM, SECRET);
            put(CATALOG_REQUEST_TYPE_TERM, CATALOG_REQUEST);
            put(DATASET_REQUEST_TYPE_TERM, DATASET_REQUEST);
            put(CONTRACT_REQUEST_TYPE_TERM, CONTRACT_REQUEST);
            put(CONTRACT_NEGOTIATION_TYPE_TERM, CONTRACT_NEGOTIATION);
            put("NegotiationState", CONTRACT_NEGOTIATION_STATE);
            put(TERMINATE_NEGOTIATION_TYPE_TERM, TERMINATE_NEGOTIATION);
            put(CONTRACT_AGREEMENT_TYPE_TERM, CONTRACT_AGREEMENT);
            put(TRANSFER_REQUEST_TYPE_TERM, TRANSFER_REQUEST);
            put(TRANSFER_PROCESS_TYPE_TERM, TRANSFER_PROCESS);
            put("TransferState", TRANSFER_PROCESS_STATE);
            put("TerminateTransfer", TERMINATE_TRANSFER);
            put("SuspendTransfer", SUSPEND_TRANSFER);


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
        schemaV4.forEach((type, schema) -> validator.register(V_4_PREFIX + type, validatorProvider.validatorFor(schema)));
    }

}
