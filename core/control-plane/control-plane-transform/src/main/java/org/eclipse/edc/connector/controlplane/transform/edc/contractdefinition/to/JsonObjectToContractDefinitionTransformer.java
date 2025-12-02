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

package org.eclipse.edc.connector.controlplane.transform.edc.contractdefinition.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_PRIVATE_PROPERTIES;

public class JsonObjectToContractDefinitionTransformer extends AbstractJsonLdTransformer<JsonObject, ContractDefinition> {

    public JsonObjectToContractDefinitionTransformer() {
        super(JsonObject.class, ContractDefinition.class);
    }

    @Override
    public @Nullable ContractDefinition transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = ContractDefinition.Builder.newInstance();
        builder.id(nodeId(object));
        visitProperties(object, (s, jsonValue) -> transformProperties(s, jsonValue, builder, context));
        return builderResult(builder::build, context);
    }

    private void transformProperties(String key, JsonValue jsonValue, ContractDefinition.Builder builder, TransformerContext context) {
        switch (key) {
            case CONTRACT_DEFINITION_ACCESSPOLICY_ID -> builder.accessPolicyId(transformString(jsonValue, context));
            case CONTRACT_DEFINITION_CONTRACTPOLICY_ID -> builder.contractPolicyId(transformString(jsonValue, context));
            case CONTRACT_DEFINITION_ASSETS_SELECTOR ->
                    builder.assetsSelector(transformArray(jsonValue, Criterion.class, context));
            case CONTRACT_DEFINITION_PRIVATE_PROPERTIES -> {
                var props = jsonValue.asJsonArray().getJsonObject(0);
                visitProperties(props, (k, val) -> transformProperties(k, val, builder, context));
            }
            default -> {
                builder.privateProperty(key, transformGenericProperty(jsonValue, context));
            }
        }
    }
}
