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

package org.eclipse.edc.protocol.dsp.catalog.dispatcher.delegate;

import okhttp3.ResponseBody;
import org.eclipse.edc.catalog.spi.DatasetRequestMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.testfixtures.dispatcher.DspHttpDispatcherDelegateTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.catalog.dispatcher.CatalogApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.catalog.dispatcher.CatalogApiPaths.DATASET_REQUEST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatasetRequestHttpRawDelegateTest extends DspHttpDispatcherDelegateTestBase<DatasetRequestMessage> {

    private DatasetRequestHttpRawDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new DatasetRequestHttpRawDelegate(serializer);
    }

    @Test
    void getMessageType_returnDatasetRequest() {
        assertThat(delegate.getMessageType()).isEqualTo(DatasetRequestMessage.class);
    }

    @Test
    void buildRequest_returnRequest() {
        var message = message();

        var httpRequest = delegate().buildRequest(message);

        assertThat(httpRequest.method()).isEqualTo("GET");
        assertThat(httpRequest.url().url()).hasToString(message.getCounterPartyAddress() + BASE_PATH + DATASET_REQUEST + "/" + message.getDatasetId());
    }

    @Test
    void parseResponse_returnDataset() throws IOException {
        var responseBody = mock(ResponseBody.class);
        var response = dummyResponseBuilder(200).body(responseBody).build();
        var bytes = "test".getBytes();

        when(responseBody.bytes()).thenReturn(bytes);
        when(responseBody.byteStream()).thenReturn(new ByteArrayInputStream(bytes));

        var result = delegate.parseResponse().apply(response);

        assertThat(result).isEqualTo(bytes);
    }

    @Test
    void parseResponse_responseBodyNull_throwException() {
        testParseResponse_shouldThrowException_whenResponseBodyNull();
    }

    @Override
    protected DspHttpDispatcherDelegate<DatasetRequestMessage, ?> delegate() {
        return delegate;
    }

    private DatasetRequestMessage message() {
        return DatasetRequestMessage.Builder.newInstance()
                .datasetId("dataset-id")
                .counterPartyAddress("http://connector")
                .protocol("protocol")
                .build();
    }
}
