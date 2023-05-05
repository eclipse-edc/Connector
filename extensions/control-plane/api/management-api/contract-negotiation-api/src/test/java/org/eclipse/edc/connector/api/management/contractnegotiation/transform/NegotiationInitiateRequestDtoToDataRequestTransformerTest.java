/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.contractnegotiation.transform;

import org.eclipse.edc.api.model.CallbackAddressDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.management.contractnegotiation.TestFunctions.createOffer;
import static org.mockito.Mockito.mock;

class NegotiationInitiateRequestDtoToDataRequestTransformerTest {

    private static final String DEFAULT_CONSUMER_ID = "urn:connector:test-consumer";
    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, UTC);

    private final NegotiationInitiateRequestDtoToDataRequestTransformer transformer = new NegotiationInitiateRequestDtoToDataRequestTransformer(clock, DEFAULT_CONSUMER_ID);

    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void verify_transform() {
        var callback = CallbackAddressDto.Builder.newInstance()
                .uri("local://test")
                .build();
        var dto = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                .consumerId("test-consumer")
                .providerId("test-provider")
                .offer(createOffer("offerId", "assetId"))
                .callbackAddresses(List.of(callback))
                .build();

        var request = transformer.transform(dto, context);

        assertThat(request).isNotNull();
        assertThat(request.getRequestData().getConnectorId()).isEqualTo("connectorId");
        assertThat(request.getRequestData().getCallbackAddress()).isEqualTo("address");
        assertThat(request.getRequestData().getProtocol()).isEqualTo("protocol");
        assertThat(request.getRequestData().getContractOffer().getId()).isEqualTo("offerId");
        assertThat(request.getRequestData().getContractOffer().getContractStart().toInstant()).isEqualTo(clock.instant());
        assertThat(request.getRequestData().getContractOffer().getContractEnd().toInstant()).isEqualTo(clock.instant().plusSeconds(dto.getOffer().getValidity()));
        assertThat(request.getRequestData().getContractOffer().getPolicy()).isNotNull();
        assertThat(request.getCallbackAddresses()).hasSize(1);
    }

    @Test
    void verify_transfor_withNoProviderId() {
        var dto = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                .consumerId("urn:connector:test-consumer")
                // do not set provider ID
                .offer(createOffer("offerId", "assetId"))
                .build();

        var request = transformer.transform(dto, context);

        assertThat(request).isNotNull();
        assertThat(request.getRequestData().getContractOffer().getProviderId()).isEqualTo(dto.getConnectorAddress());
        assertThat(request.getRequestData().getContractOffer().getConsumerId()).isEqualTo("urn:connector:test-consumer");
    }

    @Test
    void verify_transform_withNoConsumerId() {
        var dto = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                // do not set consumer ID
                .providerId("urn:connector:test-provider")
                .offer(createOffer("offerId", "assetId"))
                .build();

        var request = transformer.transform(dto, context);
        assertThat(request).isNotNull();
        assertThat(request.getRequestData().getContractOffer().getProviderId()).isEqualTo("urn:connector:test-provider");
        assertThat(request.getRequestData().getContractOffer().getConsumerId()).isEqualTo(DEFAULT_CONSUMER_ID);
    }
}
