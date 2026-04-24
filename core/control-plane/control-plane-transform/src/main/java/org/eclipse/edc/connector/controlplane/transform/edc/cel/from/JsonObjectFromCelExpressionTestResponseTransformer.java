/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.transform.edc.cel.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.cel.model.CelExpressionTestResponse;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.policy.cel.model.CelExpressionTestResponse.CEL_EXPRESSION_TEST_RESPONSE_ERROR_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpressionTestResponse.CEL_EXPRESSION_TEST_RESPONSE_EVALUATION_RESULT_IRI;
import static org.eclipse.edc.policy.cel.model.CelExpressionTestResponse.CEL_EXPRESSION_TEST_RESPONSE_TYPE_IRI;

public class JsonObjectFromCelExpressionTestResponseTransformer extends AbstractJsonLdTransformer<CelExpressionTestResponse, JsonObject> {

    private final JsonBuilderFactory factory;

    public JsonObjectFromCelExpressionTestResponseTransformer(JsonBuilderFactory factory) {
        super(CelExpressionTestResponse.class, JsonObject.class);
        this.factory = factory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull CelExpressionTestResponse response, @NotNull TransformerContext context) {
        var builder = factory.createObjectBuilder().add(TYPE, CEL_EXPRESSION_TEST_RESPONSE_TYPE_IRI);
        if (response.getEvaluationResult() != null) {
            builder.add(CEL_EXPRESSION_TEST_RESPONSE_EVALUATION_RESULT_IRI, response.getEvaluationResult());
        }
        if (response.getError() != null) {
            builder.add(CEL_EXPRESSION_TEST_RESPONSE_ERROR_IRI, response.getError());
        }
        return builder.build();
    }
}
