/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.contractagreement.to;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.RetireAgreement;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.RetireAgreement.RETIRE_AGREEMENT_REASON;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.RetireAgreement.RETIRE_AGREEMENT_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JsonObjectToRetireAgreementTransformerTest {

    private final JsonObjectToRetireAgreementTransformer transformer = new JsonObjectToRetireAgreementTransformer();
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(RetireAgreement.class);
    }

    @Test
    void transform() {
        var json = Json.createObjectBuilder()
                .add(TYPE, RETIRE_AGREEMENT_TYPE)
                .add(RETIRE_AGREEMENT_REASON, value("a reason"))
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.reason()).isEqualTo("a reason");
        verifyNoInteractions(context);
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}
