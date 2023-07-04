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

package org.eclipse.edc.protocol.dsp.negotiation.dispatcher.delegate;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.testfixtures.dispatcher.DspHttpDispatcherDelegateTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.negotiation.dispatcher.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.dispatcher.NegotiationApiPaths.CONTRACT_OFFER;

class ContractOfferMessageHttpDelegateTest extends DspHttpDispatcherDelegateTestBase<ContractOfferMessage> {
    
    private ContractOfferMessageHttpDelegate delegate;
    
    @BeforeEach
    void setUp() {
        delegate = new ContractOfferMessageHttpDelegate(serializer);
    }
    
    @Test
    void getMessageType() {
        assertThat(delegate.getMessageType()).isEqualTo(ContractOfferMessage.class);
    }

    @Test
    void buildRequest() throws IOException {
        var message = message();
        testBuildRequest_shouldReturnRequest(message, BASE_PATH + message.getProcessId() + CONTRACT_OFFER);
    }
    
    @Test
    void buildRequest_serializationFails_throwException() {
        testBuildRequest_shouldThrowException_whenSerializationFails(message());
    }
    
    @Test
    void parseResponse_shouldReturnNullFunction() {
        testParseResponse_shouldReturnNullFunction_whenResponseBodyNotProcessed();
    }
    
    @Override
    protected DspHttpDispatcherDelegate<ContractOfferMessage, ?> delegate() {
        return delegate;
    }
    
    private ContractOfferMessage message() {
        return ContractOfferMessage.Builder.newInstance()
                .protocol("dsp")
                .processId("processId")
                .counterPartyAddress("http://connector")
                .contractOffer(contractOffer())
                .callbackAddress("http://callback")
                .build();
    }
    
    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(randomUUID().toString())
                .assetId("assetId")
                .policy(Policy.Builder.newInstance().build())
                .build();
    }
}
