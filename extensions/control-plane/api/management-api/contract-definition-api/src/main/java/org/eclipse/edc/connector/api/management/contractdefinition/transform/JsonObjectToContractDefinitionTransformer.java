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
 *
 */

package org.eclipse.edc.connector.api.management.contractdefinition.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;


public class JsonObjectToContractDefinitionTransformer extends AbstractJsonLdTransformer<JsonObject, ContractDefinition> {

    public JsonObjectToContractDefinitionTransformer() {
        super(JsonObject.class, ContractDefinition.class);
    }

    @Override
    public @Nullable ContractDefinition transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = ContractDefinition.Builder.newInstance();

        builder.id(nodeId(object));

        visitProperties(object, (key, value) -> {
            switch (key) {
                case CONTRACT_DEFINITION_ACCESSPOLICY_ID -> builder.accessPolicyId(transformString(value, context));
                case CONTRACT_DEFINITION_CONTRACTPOLICY_ID -> builder.contractPolicyId(transformString(value, context));
                case CONTRACT_DEFINITION_ASSETS_SELECTOR -> builder.assetsSelector(transformArray(value, Criterion.class, context));
                default -> { }
            }
        });

        return builderResult(builder::build, context);
    }

}
