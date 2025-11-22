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

package org.eclipse.edc.connector.controlplane.transform.edc.policy.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_POLICY;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_PRIVATE_PROPERTIES;

public class JsonObjectToPolicyDefinitionTransformer extends AbstractJsonLdTransformer<JsonObject, PolicyDefinition> {

    public JsonObjectToPolicyDefinitionTransformer() {
        super(JsonObject.class, PolicyDefinition.class);
    }

    @Override
    public @Nullable PolicyDefinition transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var builder = PolicyDefinition.Builder.newInstance();
        builder.id(nodeId(input));

        var policy = Optional.of(EDC_POLICY_DEFINITION_POLICY)
                .map(input::get)
                .map(it -> transformObject(it, Policy.class, context))
                .orElse(null);

        if (policy == null) {
            return null;
        } else {
            builder.policy(policy);
        }

        var privateProperties = input.get(EDC_POLICY_DEFINITION_PRIVATE_PROPERTIES);
        if (privateProperties != null) {
            var props = privateProperties.asJsonArray().getJsonObject(0);
            visitProperties(props, (key, value) -> builder.privateProperty(key, transformGenericProperty(value, context)));
        }

        return builder.build();
    }

}
