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

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.protocol.dsp.negotiation.transform.ContractNegotiationError;
import org.eclipse.edc.protocol.dsp.spi.mapper.DspHttpStatusCodeMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonObjectFromContractNegotiationErrorTransformerTest {

    private JsonObjectFromContractNegotiationErrorTransformer transformer;

    private TransformerContext context = mock(TransformerContext.class);

    private DspHttpStatusCodeMapper statusCodeMapper = mock(DspHttpStatusCodeMapper.class);

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromContractNegotiationErrorTransformer(statusCodeMapper);
    }

    @Test
    void transferErrorToResponseWithId() {
        when(statusCodeMapper.mapErrorToStatusCode(any(InvalidRequestException.class))).thenReturn(400);

        var contractNegotiationError = new ContractNegotiationError(Optional.of("testId"), new InvalidRequestException("testError"));

        var result = transformer.transform(contractNegotiationError, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(result.getJsonString(DSPACE_NEGOTIATION_PROPERTY_CODE).getString()).isEqualTo("400");
        assertThat(result.getJsonString(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID).getString()).isEqualTo("testId");
        assertThat(result.get(DSPACE_NEGOTIATION_PROPERTY_REASON)).isNotNull();

    }

    @Test
    void transferErrorToResponseWithoutId() {
        when(statusCodeMapper.mapErrorToStatusCode(any(InvalidRequestException.class))).thenReturn(400);

        var contractNegotiationError = new ContractNegotiationError(Optional.empty(), new InvalidRequestException("testError"));

        var result = transformer.transform(contractNegotiationError, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(result.getJsonString(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID).getString()).isEqualTo("InvalidId");
        assertThat(result.getJsonString(DSPACE_NEGOTIATION_PROPERTY_CODE).getString()).isEqualTo("400");
        assertThat(result.get(DSPACE_NEGOTIATION_PROPERTY_REASON)).isNotNull();
    }

    @Test
    void transferErrorWithoutReason() {
        when(statusCodeMapper.mapErrorToStatusCode(any(Exception.class))).thenReturn(500);

        var contractNegotiationError = new ContractNegotiationError(Optional.of("testId"), new Exception());

        var result = transformer.transform(contractNegotiationError, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_CONTRACT_NEGOTIATION_ERROR);
        assertThat(result.getJsonString(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID).getString()).isEqualTo("testId");
        assertThat(result.getJsonString(DSPACE_NEGOTIATION_PROPERTY_CODE).getString()).isEqualTo("500");
        assertThat(result.containsKey(DSPACE_NEGOTIATION_PROPERTY_REASON)).isFalse();
    }
}
