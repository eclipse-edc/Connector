/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.odrl.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_TYPE;

/**
 * Converts from an ODRL operator as a {@link JsonObject} in JSON-LD expanded form to an {@link Operator}.
 */
public class JsonObjectToOperatorTransformer extends AbstractJsonLdTransformer<JsonObject, Operator> {

    public JsonObjectToOperatorTransformer() {
        super(JsonObject.class, Operator.class);
    }

    @Override
    public @Nullable Operator transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var value = nodeId(object);

        if (value == null) {
            context.problem().missingProperty().property(ID).type(ODRL_OPERATOR_TYPE).report();
            return null;
        }

        return Arrays.stream(Operator.values())
                .filter(it -> it.getOdrlRepresentation().equals(value))
                .findFirst()
                .orElseGet(() -> {
                    context.problem().invalidProperty().property(ID).type(ODRL_OPERATOR_TYPE).value(value).report();
                    return null;
                });

    }

}
