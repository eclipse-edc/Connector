/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.api.management.policy.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyEvaluationPlanRequest;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyEvaluationPlanRequest.EDC_POLICY_EVALUATION_PLAN_REQUEST_POLICY_SCOPE;
import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyEvaluationPlanRequest.EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.Mockito.mock;

public class JsonObjectToPolicyEvaluationPlanRequestTransformerTest {

    private final JsonObjectToPolicyEvaluationPlanRequestTransformer transformer = new JsonObjectToPolicyEvaluationPlanRequestTransformer();
    private final TransformerContext context = mock(TransformerContext.class);
    private final TitaniumJsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(PolicyEvaluationPlanRequest.class);
    }

    @Test
    void transform() {
        var json = createObjectBuilder()
                .add(TYPE, EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE)
                .add(EDC_POLICY_EVALUATION_PLAN_REQUEST_POLICY_SCOPE, "scope")
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result).isNotNull();
        assertThat(result.policyScope()).isEqualTo("scope");
    }

    private JsonObject expand(JsonObject jsonObject) {
        return jsonLd.expand(jsonObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
    }
}
