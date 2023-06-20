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

package org.eclipse.edc.connector.api.management.policy.transform;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionUpdateDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionRequestDto.EDC_POLICY_DEFINITION_POLICY;

public class JsonObjectToPolicyDefinitionUpdateDtoTransformer extends AbstractJsonLdTransformer<JsonObject, PolicyDefinitionUpdateDto> {

    public JsonObjectToPolicyDefinitionUpdateDtoTransformer() {
        super(JsonObject.class, PolicyDefinitionUpdateDto.class);
    }

    @Override
    public @Nullable PolicyDefinitionUpdateDto transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var builder = PolicyDefinitionUpdateDto.Builder.newInstance();
        visitProperties(input, (key, value) -> transformProperty(key, value, builder, context));
        return builder.build();
    }

    private void transformProperty(String key, JsonValue value, PolicyDefinitionUpdateDto.Builder builder, TransformerContext context) {
        if (key.equals(EDC_POLICY_DEFINITION_POLICY)) {
            transformArrayOrObject(value, Policy.class, builder::policy, context);
        }
    }
}
