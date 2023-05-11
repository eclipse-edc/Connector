/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_CONTRACT_NEGOTIATION_ERROR;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_REASON;
import static org.mockito.Mockito.mock;

public class ContractNegotiationErrorToResponseTransformerTest {

    private ContractNegotiationErrorToResponseTransformer transformer;

    private TransformerContext context = mock(TransformerContext.class);

    @BeforeEach
    void setUp() {
        transformer = new ContractNegotiationErrorToResponseTransformer();
    }

    @Test
    void transferErrorToResponseWithId() {
        var contractNegotiationError = new ContractNegotiationError(Optional.of("testId"), new InvalidRequestException("testError"));

        var result = transformer.transform(contractNegotiationError, context);

        assertThat(result).isNotNull();

        assertThat(result.getStatus()).isEqualTo(400);

        assertThat(result.getEntity()).isInstanceOf(JsonObject.class);
        var jsonObject = (JsonObject) result.getEntity();

        assertThat(jsonObject.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(jsonObject.getJsonString(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID).getString()).isEqualTo("testId");
        assertThat(jsonObject.getJsonString(DSPACE_NEGOTIATION_PROPERTY_CODE).getString()).isEqualTo("400");
        assertThat(jsonObject.get(DSPACE_NEGOTIATION_PROPERTY_REASON)).isNotNull();

    }

    @Test
    void transferErrorToResponseWithoutId() {
        var contractNegotiationError = new ContractNegotiationError(Optional.empty(), new InvalidRequestException("testError"));

        var result = transformer.transform(contractNegotiationError, context);

        assertThat(result).isNotNull();

        assertThat(result.getStatus()).isEqualTo(400);

        assertThat(result.getEntity()).isInstanceOf(JsonObject.class);
        var jsonObject = (JsonObject) result.getEntity();

        assertThat(jsonObject.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(jsonObject.getJsonString(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID).getString()).isEqualTo("null");
        assertThat(jsonObject.getJsonString(DSPACE_NEGOTIATION_PROPERTY_CODE).getString()).isEqualTo("400");
        assertThat(jsonObject.get(DSPACE_NEGOTIATION_PROPERTY_REASON)).isNotNull();
    }

    @Test
    void transferErrorWithoutReason() {
        var contractNegotiationError = new ContractNegotiationError(Optional.of("testId"), new Throwable());

        var result = transformer.transform(contractNegotiationError, context);

        assertThat(result).isNotNull();

        assertThat(result.getStatus()).isEqualTo(500);

        assertThat(result.getEntity()).isInstanceOf(JsonObject.class);
        var jsonObject = (JsonObject) result.getEntity();

        assertThat(jsonObject.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(jsonObject.getJsonString(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID).getString()).isEqualTo("testId");
        assertThat(jsonObject.getJsonString(DSPACE_NEGOTIATION_PROPERTY_CODE).getString()).isEqualTo("500");
        assertThat(!jsonObject.containsKey(DSPACE_NEGOTIATION_PROPERTY_REASON));
    }
}
