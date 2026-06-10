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

import jakarta.json.JsonObject;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V4;
import org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.V5;
import org.eclipse.edc.connector.api.management.schema.CustomSchemaValidatorConfigParser.CustomValidatorGroup;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.DSPACE_2025_SCHEMA_PREFIX;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.EDC_MGMT_V4_SCHEMA_PREFIX;
import static org.eclipse.edc.api.management.schema.ManagementApiJsonSchema.EDC_MGMT_V5_SCHEMA_PREFIX;
import static org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension.NAME;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_CATALOG_ASSET_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.NegotiationState.NEGOTIATION_STATE_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.TerminateNegotiation.TERMINATE_NEGOTIATION_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE_TERM;
import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance.DATAPLANE_INSTANCE_TYPE_TERM;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TYPE_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_TYPE_TERM;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_TYPE_TERM;
import static org.eclipse.edc.policy.cel.model.CelExpression.CEL_EXPRESSION_TYPE_TERM;
import static org.eclipse.edc.policy.cel.model.CelExpressionTestRequest.CEL_EXPRESSION_TEST_REQUEST_TYPE_TERM;
import static org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan.EDC_POLICY_EVALUATION_PLAN_TYPE_TERM;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.spi.types.domain.secret.Secret.EDC_SECRET_TYPE_TERM;

@Extension(NAME)
public class ManagementApiSchemaValidatorExtension implements ServiceExtension {

    public static final String NAME = "Management API Schema Validator";
    public static final String V_4_PREFIX = "v4:";
    public static final String V_5_PREFIX = "v5:";
    public static final String CONFIG_PREFIX = "edc.mgmt.api.schema";
    public static final String VALIDATOR_KEY = "validator";

    @Setting(context = CONFIG_PREFIX + ".<groupAlias>.", description = "Namespace prefix used when registering custom validators on JsonObjectValidatorRegistry, e.g. 'v4'.")
    public static final String VERSION_KEY = "version";
    @Setting(context = CONFIG_PREFIX + ".<groupAlias>.", description = "Optional schema URL prefix to remap to a local location (paired with 'mapping.to').", required = false)
    public static final String MAPPING_FROM_KEY = "mapping.from";
    @Setting(context = CONFIG_PREFIX + ".<groupAlias>.", description = "Optional local URI the schema prefix is remapped to, e.g. 'file:///path' or 'classpath:/path' (paired with 'mapping.from').", required = false)
    public static final String MAPPING_TO_KEY = "mapping.to";
    @Setting(context = CONFIG_PREFIX + ".<groupAlias>." + VALIDATOR_KEY + ".<entryAlias>.", description = "JSON-LD @type term the schema is registered against.")
    public static final String VALIDATOR_TYPE_KEY = "type";
    @Setting(context = CONFIG_PREFIX + ".<groupAlias>." + VALIDATOR_KEY + ".<entryAlias>.", description = "Absolute schema URL (resolved through 'mapping.from'/'mapping.to' when configured).")
    public static final String VALIDATOR_SCHEMA_KEY = "schema";

    @Setting(context = CONFIG_PREFIX + ".<groupAlias>." + VALIDATOR_KEY + ".<entryAlias>.", description = "Optional profile to associate with the validator in order to be activated", required = false)
    public static final String VALIDATOR_PROFILES_KEY = "profiles";

    private static final String EDC_CLASSPATH_SCHEMA_V4 = "classpath:schema/management/v4";
    private static final String EDC_CLASSPATH_SCHEMA_V5 = "classpath:schema/management/v5";
    private static final String DSPACE_CLASSPATH_SCHEMA = "classpath:schema/dspace/2025";

    private final Map<String, String> schemaV4 = new HashMap<>() {
        {
            put("IdResponse", V4.ID_RESPONSE);
            put(EDC_ASSET_TYPE_TERM, V4.ASSET);
            put(EDC_CATALOG_ASSET_TYPE_TERM, V4.CATALOG_ASSET);
            put(EDC_QUERY_SPEC_TYPE_TERM, V4.QUERY_SPEC);
            put(EDC_POLICY_DEFINITION_TYPE_TERM, V4.POLICY_DEFINITION);
            put(CONTRACT_DEFINITION_TYPE_TERM, V4.CONTRACT_DEFINITION);
            put(DATAPLANE_INSTANCE_TYPE_TERM, V4.DATAPLANE_INSTANCE);
            put(EDR_ENTRY_TYPE_TERM, V4.EDR_ENTRY);
            put("PolicyEvaluationPlanRequest", V4.POLICY_EVALUATION_REQUEST);
            put(EDC_POLICY_EVALUATION_PLAN_TYPE_TERM, V4.POLICY_EVALUATION_PLAN);
            put("PolicyValidationResult", V4.POLICY_VALIDATION_RESULT);
            put(EDC_SECRET_TYPE_TERM, V4.SECRET);
            put(CATALOG_REQUEST_TYPE_TERM, V4.CATALOG_REQUEST);
            put(DATASET_REQUEST_TYPE_TERM, V4.DATASET_REQUEST);
            put(CONTRACT_REQUEST_TYPE_TERM, V4.CONTRACT_REQUEST);
            put(CONTRACT_NEGOTIATION_TYPE_TERM, V4.CONTRACT_NEGOTIATION);
            put(NEGOTIATION_STATE_TYPE_TERM, V4.CONTRACT_NEGOTIATION_STATE);
            put(TERMINATE_NEGOTIATION_TYPE_TERM, V4.TERMINATE_NEGOTIATION);
            put(CONTRACT_AGREEMENT_TYPE_TERM, V4.CONTRACT_AGREEMENT);
            put(TRANSFER_REQUEST_TYPE_TERM, V4.TRANSFER_REQUEST);
            put(TRANSFER_PROCESS_TYPE_TERM, V4.TRANSFER_PROCESS);
            put("TransferState", V4.TRANSFER_PROCESS_STATE);
            put("TerminateTransfer", V4.TERMINATE_TRANSFER);
            put("SuspendTransfer", V4.SUSPEND_TRANSFER);
            put(PARTICIPANT_CONTEXT_TYPE_TERM, V4.PARTICIPANT_CONTEXT);
            put(PARTICIPANT_CONTEXT_CONFIG_TYPE_TERM, V4.PARTICIPANT_CONTEXT_CONFIG);
            put(CEL_EXPRESSION_TYPE_TERM, V4.CEL_EXPRESSION);
            put(CEL_EXPRESSION_TEST_REQUEST_TYPE_TERM, V4.CEL_EXPRESSION_TEST_REQUEST);
            put("AssociateDataspaceProfile", V4.ASSOCIATE_DATASPACE_PROFILE_CONTEXT);
            put("DiscoveryRequest", V4.DISCOVERY_REQUEST);
        }
    };

    private final Map<String, String> schemaV5 = new HashMap<>() {
        {
            put(EDC_ASSET_TYPE_TERM, V5.ASSET);
            put(EDC_CATALOG_ASSET_TYPE_TERM, V5.CATALOG_ASSET);
        }
    };

    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonObjectValidatorRegistry validator;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var customGroups = CustomSchemaValidatorConfigParser.parse(context.getConfig(CONFIG_PREFIX), context.getMonitor());

        var builder = ManagementApiSchemaValidatorProvider.Builder.newInstance()
                .objectMapper(() -> typeManager.getMapper(JSON_LD))
                .prefixMapping(EDC_MGMT_V4_SCHEMA_PREFIX, EDC_CLASSPATH_SCHEMA_V4)
                .prefixMapping(EDC_MGMT_V5_SCHEMA_PREFIX, EDC_CLASSPATH_SCHEMA_V5)
                .prefixMapping(DSPACE_2025_SCHEMA_PREFIX, DSPACE_CLASSPATH_SCHEMA);

        customGroups.stream()
                .map(CustomValidatorGroup::mapping)
                .filter(Objects::nonNull)
                .forEach(mapping -> builder.prefixMapping(mapping.from(), mapping.to()));

        var schemaValidatorProvider = builder.build();

        schemaV4.forEach((type, schema) -> validator.register(V_4_PREFIX + type, schemaValidatorProvider.validatorFor(schema)));
        schemaV5.forEach((type, schema) -> validator.register(V_5_PREFIX + type, schemaValidatorProvider.validatorFor(schema)));
        registerCustomValidators(schemaValidatorProvider, customGroups);
    }

    void registerCustomValidators(ManagementApiSchemaValidatorProvider validatorProvider, List<CustomValidatorGroup> groups) {
        groups.forEach(group -> group.bindings().forEach(binding ->
                validator.register(group.version() + ":" + binding.type(), validatorForSchema(validatorProvider, binding.schema(), binding.type(), binding.profiles()))));
    }

    private Validator<JsonObject> validatorForSchema(ManagementApiSchemaValidatorProvider validatorProvider, String schema, String type, List<String> profiles) {
        var validator = validatorProvider.validatorFor(schema);
        if (profiles.isEmpty()) {
            return validator;
        }
        return (input) -> {
            var profile = extractProfile(input, type);
            // Skip validation if the input contains a profile that is not associated with the validator; otherwise, validate as normal.
            // This allows for multiple validators to be registered for the same type but with different profiles.
            if (profile == null || !profiles.contains(profile)) {
                return ValidationResult.success();
            } else {
                return validator.validate(input);
            }
        };
    }

    // Currently hardcoded only for PolicyDefinition, but can be extended to support other types and profile locations as needed
    private String extractProfile(JsonObject input, String type) {
        var inputType = input.getString(TYPE, null);
        if (type.equals(EDC_POLICY_DEFINITION_TYPE_TERM) && EDC_POLICY_DEFINITION_TYPE_TERM.equals(inputType)) {
            var policy = input.getJsonObject("policy");
            if (policy != null) {
                return policy.getString("profile", null);
            }
        }
        return null;
    }
}
