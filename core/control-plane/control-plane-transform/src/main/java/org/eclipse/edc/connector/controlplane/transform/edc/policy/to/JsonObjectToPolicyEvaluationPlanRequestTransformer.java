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
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyEvaluationPlanRequest;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyEvaluationPlanRequest.EDC_POLICY_EVALUATION_PLAN_REQUEST_POLICY_SCOPE;

public class JsonObjectToPolicyEvaluationPlanRequestTransformer extends AbstractJsonLdTransformer<JsonObject, PolicyEvaluationPlanRequest> {

    public JsonObjectToPolicyEvaluationPlanRequestTransformer() {
        super(JsonObject.class, PolicyEvaluationPlanRequest.class);
    }

    @Override
    public @Nullable PolicyEvaluationPlanRequest transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var policyScope = transformString(input.get(EDC_POLICY_EVALUATION_PLAN_REQUEST_POLICY_SCOPE), context);
        return new PolicyEvaluationPlanRequest(policyScope);
    }

}
