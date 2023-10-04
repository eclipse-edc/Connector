/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.util.sink;

import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class OutputStreamDataSinkFactoryTest {

    private OutputStreamDataSinkFactory factory;

    @BeforeEach
    void setUp() {
        factory = new OutputStreamDataSinkFactory();
    }

    @Test
    void verifyCanHandle() {
        assertThat(factory.canHandle(createDataFlowRequest(OutputStreamDataSinkFactory.TYPE))).isTrue();
        assertThat(factory.canHandle(createDataFlowRequest("dummy"))).isFalse();
    }

    @Test
    void validate_shouldSucceed_whenRequestIsManageable() {
        var request = createDataFlowRequest(OutputStreamDataSinkFactory.TYPE);

        var result = factory.validateRequest(request);

        assertThat(result).isSucceeded();
    }

    @Test
    void validate_shouldFail_whenRequestIsNotManageable() {
        var request = createDataFlowRequest("dummy");

        var result = factory.validateRequest(request);

        assertThat(result).isFailed().extracting(Failure::getMessages).asList()
                .containsExactly("OutputStreamDataSinkFactory: Cannot handle destination data address with type: dummy");
    }

    @Test
    void verifyCreateSinkReturnCompletedFuture() {
        var sink = factory.createSink(null);

        assertThat(sink.transfer(null)).succeedsWithin(500L, TimeUnit.MILLISECONDS);
    }

    private static DataFlowRequest createDataFlowRequest(String destAddressType) {
        return DataFlowRequest.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(DataAddress.Builder.newInstance()
                        .type("")
                        .build())
                .destinationDataAddress(DataAddress.Builder.newInstance()
                        .type(destAddressType)
                        .build())
                .build();
    }
}
