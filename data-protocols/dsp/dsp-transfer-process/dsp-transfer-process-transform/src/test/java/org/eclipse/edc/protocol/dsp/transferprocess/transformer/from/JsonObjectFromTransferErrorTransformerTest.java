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

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.from;

import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.protocol.dsp.spi.mapper.DspHttpStatusCodeMapper;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.TransferError;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferErrorTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFER_PROCESS_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonObjectFromTransferErrorTransformerTest {

    private JsonObjectFromTransferErrorTransformer transformer;

    private TransformerContext context = mock(TransformerContext.class);

    private DspHttpStatusCodeMapper statusCodeMapper = mock(DspHttpStatusCodeMapper.class);

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromTransferErrorTransformer(statusCodeMapper);
    }

    @Test
    void transferErrorToResponseWithId() {
        when(statusCodeMapper.mapErrorToStatusCode(any(InvalidRequestException.class))).thenReturn(400);

        var transferError = new TransferError(Optional.of("testId"), new InvalidRequestException("testError"));

        var result = transformer.transform(transferError, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_TRANSFER_PROCESS_ERROR);
        assertThat(result.getJsonString(DSPACE_PROCESSID_TYPE).getString()).isEqualTo("testId");
        assertThat(result.getJsonString(DSPACE_SCHEMA + "code").getString()).isEqualTo("400");
        assertThat(result.get(DSPACE_SCHEMA + "reason")).isNotNull();
    }

    @Test
    void transferErrorToResponseWithoutId() {
        when(statusCodeMapper.mapErrorToStatusCode(any(InvalidRequestException.class))).thenReturn(400);

        var transferError = new TransferError(Optional.empty(), new InvalidRequestException("testError"));

        var result = transformer.transform(transferError, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_TRANSFER_PROCESS_ERROR);
        assertThat(result.getJsonString(DSPACE_PROCESSID_TYPE).getString()).isEqualTo("InvalidId");
        assertThat(result.getJsonString(DSPACE_SCHEMA + "code").getString()).isEqualTo("400");
        assertThat(result.get(DSPACE_SCHEMA + "reason")).isNotNull();
    }

    @Test
    void transferErrorWithoutReason() {
        when(statusCodeMapper.mapErrorToStatusCode(any(Exception.class))).thenReturn(500);

        var transferError = new TransferError(Optional.of("testId"), new Exception());

        var result = transformer.transform(transferError, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_TRANSFER_PROCESS_ERROR);
        assertThat(result.getJsonString(DSPACE_PROCESSID_TYPE).getString()).isEqualTo("testId");
        assertThat(result.getJsonString(DSPACE_SCHEMA + "code").getString()).isEqualTo("500");
        assertThat(result.containsKey(DSPACE_SCHEMA + "reason")).isFalse();
    }
}
