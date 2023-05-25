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
import jakarta.json.JsonValue;
import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;

public class JsonObjectToContractDefinitionRequestDtoTransformer extends AbstractJsonLdTransformer<JsonObject, ContractDefinitionRequestDto> {

    public JsonObjectToContractDefinitionRequestDtoTransformer() {
        super(JsonObject.class, ContractDefinitionRequestDto.class);
    }

    @Override
    public @Nullable ContractDefinitionRequestDto transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = ContractDefinitionRequestDto.Builder.newInstance();

        builder.id(nodeId(object));
        visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));
        return builderResult(builder::build, context);
    }

    private void transformProperties(String key, JsonValue value, ContractDefinitionRequestDto.Builder builder, TransformerContext context) {
        if (CONTRACT_DEFINITION_ACCESSPOLICY_ID.equals(key)) {
            transformString(value, builder::accessPolicyId, context);
        } else if (CONTRACT_DEFINITION_CONTRACTPOLICY_ID.equals(key)) {
            transformString(value, builder::contractPolicyId, context);
        } else if (CONTRACT_DEFINITION_ASSETS_SELECTOR.equals(key)) {
            var list = new ArrayList<CriterionDto>();
            transformArrayOrObject(value, CriterionDto.class, list::add, context);
            builder.assetsSelector(list);
        }
    }
}
