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

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto.CONTRACT_DEFINITION_CRITERIA;
import static org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromContractDefinitionResponseDtoTransformer extends AbstractJsonLdTransformer<ContractDefinitionResponseDto, JsonObject> {
    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractDefinitionResponseDtoTransformer(JsonBuilderFactory jsonFactory) {
        super(ContractDefinitionResponseDto.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractDefinitionResponseDto dto, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(ID, dto.getId())
                .add(TYPE, CONTRACT_DEFINITION_TYPE)
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, dto.getAccessPolicyId())
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, dto.getContractPolicyId());

        // todo: does the criterionDto need to be json-ld-ified?
        var criteria = dto.getCriteria().stream()
                .map(criterion -> context.transform(criterion, JsonObject.class))
                .collect(jsonFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();

        builder.add(CONTRACT_DEFINITION_CRITERIA, criteria);
        return builder.build();
    }
}
