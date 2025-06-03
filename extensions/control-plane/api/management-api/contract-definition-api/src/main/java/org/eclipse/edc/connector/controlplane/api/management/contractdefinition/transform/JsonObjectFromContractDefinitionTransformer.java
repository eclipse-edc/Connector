/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       SAP SE - add private properties to contract definition
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition.transform;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.EDC_CREATED_AT;

public class JsonObjectFromContractDefinitionTransformer extends AbstractJsonLdTransformer<ContractDefinition, JsonObject> {
    private final TypeManager typeManager;
    private final String typeContext;
    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractDefinitionTransformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext) {
        super(ContractDefinition.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractDefinition contractDefinition, @NotNull TransformerContext context) {
        var criteria = contractDefinition.getAssetsSelector().stream()
                .map(criterion -> context.transform(criterion, JsonObject.class))
                .collect(toJsonArray());

        var builder = jsonFactory.createObjectBuilder()
                .add(ID, contractDefinition.getId())
                .add(TYPE, CONTRACT_DEFINITION_TYPE)
                .add(EDC_CREATED_AT, contractDefinition.getCreatedAt())
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, contractDefinition.getAccessPolicyId())
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, contractDefinition.getContractPolicyId())
                .add(CONTRACT_DEFINITION_ASSETS_SELECTOR, criteria);

        if (!contractDefinition.getPrivateProperties().isEmpty()) {
            var privatePropBuilder = jsonFactory.createObjectBuilder();
            transformProperties(contractDefinition.getPrivateProperties(), privatePropBuilder, typeManager.getMapper(typeContext), context);
            builder.add(ContractDefinition.CONTRACT_DEFINITION_PRIVATE_PROPERTIES, privatePropBuilder);
        }

        return builder.build();
    }
}
