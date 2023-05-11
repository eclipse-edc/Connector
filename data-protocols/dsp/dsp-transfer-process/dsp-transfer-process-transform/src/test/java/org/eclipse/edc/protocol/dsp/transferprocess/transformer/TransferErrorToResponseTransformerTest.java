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

package org.eclipse.edc.protocol.dsp.transferprocess.transformer;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFER_PROCESS_ERROR;
import static org.mockito.Mockito.mock;

public class TransferErrorToResponseTransformerTest {

    private TransferErrorToResponseTransformer transformer;

    private TransformerContext context = mock(TransformerContext.class);

    @BeforeEach
    void setUp() {
        transformer = new TransferErrorToResponseTransformer();
    }

    @Test
    void transferErrorToResponseWithId() {
        var transferError = new TransferError(Optional.of("testId"), new InvalidRequestException("testError"));

        var result = transformer.transform(transferError, context);

        assertThat(result).isNotNull();

        assertThat(result.getStatus()).isEqualTo(400);

        assertThat(result.getEntity()).isInstanceOf(JsonObject.class);
        var jsonObject = (JsonObject) result.getEntity();

        assertThat(jsonObject.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_TRANSFER_PROCESS_ERROR);
        assertThat(jsonObject.getJsonString(DSPACE_PROCESSID_TYPE).getString()).isEqualTo("testId");
        assertThat(jsonObject.getJsonString(DSPACE_SCHEMA + "code").getString()).isEqualTo("400");
        assertThat(jsonObject.get(DSPACE_SCHEMA + "reason")).isNotNull();

    }

    @Test
    void transferErrorToResponseWithoutId() {
        var transferError = new TransferError(Optional.empty(), new InvalidRequestException("testError"));

        var result = transformer.transform(transferError, context);

        assertThat(result).isNotNull();

        assertThat(result.getStatus()).isEqualTo(400);

        assertThat(result.getEntity()).isInstanceOf(JsonObject.class);
        var jsonObject = (JsonObject) result.getEntity();

        assertThat(jsonObject.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_TRANSFER_PROCESS_ERROR);
        assertThat(jsonObject.getJsonString(DSPACE_PROCESSID_TYPE).getString()).isEqualTo("null");
        assertThat(jsonObject.getJsonString(DSPACE_SCHEMA + "code").getString()).isEqualTo("400");
        assertThat(jsonObject.get(DSPACE_SCHEMA + "reason")).isNotNull();
    }

    @Test
    void transferErrorWithoutReason() {
        var transferError = new TransferError(Optional.of("testId"), new Throwable());

        var result = transformer.transform(transferError, context);

        assertThat(result).isNotNull();

        assertThat(result.getStatus()).isEqualTo(500);

        assertThat(result.getEntity()).isInstanceOf(JsonObject.class);
        var jsonObject = (JsonObject) result.getEntity();

        assertThat(jsonObject.getJsonString(JsonLdKeywords.TYPE).getString()).isEqualTo(DSPACE_TRANSFER_PROCESS_ERROR);
        assertThat(jsonObject.getJsonString(DSPACE_PROCESSID_TYPE).getString()).isEqualTo("testId");
        assertThat(jsonObject.getJsonString(DSPACE_SCHEMA + "code").getString()).isEqualTo("500");
        assertThat(!jsonObject.containsKey(DSPACE_SCHEMA + "reason"));
    }
}
