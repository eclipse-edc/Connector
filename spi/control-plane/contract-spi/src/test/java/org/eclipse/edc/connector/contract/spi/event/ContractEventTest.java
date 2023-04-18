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

package org.eclipse.edc.connector.contract.spi.event;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.eclipse.edc.connector.contract.spi.event.contractdefinition.ContractDefinitionCreated;
import org.eclipse.edc.connector.contract.spi.event.contractdefinition.ContractDefinitionDeleted;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationAccepted;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationConfirmed;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationDeclined;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationFailed;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationInitiated;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationOffered;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationTerminated;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ContractEventTest {

    private final TypeManager typeManager = new TypeManager();

    @ParameterizedTest
    @ArgumentsSource(EventInstances.class)
    void serdes(EventEnvelope<?> event) {
        var eventEnvelopeClass = event.getClass();
        var eventClass = event.getPayload().getClass();

        typeManager.registerTypes(new NamedType(eventEnvelopeClass, eventEnvelopeClass.getSimpleName()));
        typeManager.registerTypes(new NamedType(eventClass, eventClass.getSimpleName()));

        var json = typeManager.writeValueAsString(event);
        var deserialized = typeManager.readValue(json, EventEnvelope.class);

        assertThat(deserialized)
                .isInstanceOf(eventEnvelopeClass)
                .usingRecursiveComparison().isEqualTo(event);
    }


    private static class EventInstances implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var eventBuilders = Stream.of(
                    ContractDefinitionCreated.Builder.newInstance().contractDefinitionId("id").build(),
                    ContractDefinitionDeleted.Builder.newInstance().contractDefinitionId("id").build(),
                    baseBuilder(ContractNegotiationAccepted.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationConfirmed.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationDeclined.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationFailed.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationInitiated.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationOffered.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationRequested.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationTerminated.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationFinalized.Builder.newInstance())
                            .contractAgreement(ContractAgreement.Builder.newInstance()
                                    .id("test")
                                    .providerAgentId("provider")
                                    .consumerAgentId("consumer")
                                    .assetId("assetId")
                                    .policy(Policy.Builder.newInstance().build()).build())
                            .build()
            );

            return eventBuilders
                    .map(it -> EventEnvelope.Builder.newInstance()
                            .at(Clock.systemUTC().millis())
                            .id(UUID.randomUUID().toString()).payload(it)
                            .build())
                    .map(Arguments::of);
        }

        private <T extends ContractNegotiationEvent, B extends ContractNegotiationEvent.Builder<T, B>> B baseBuilder(B builder) {
            var callbacks = List.of(CallbackAddress.Builder.newInstance().uri("http://local").events(Set.of("test")).build());
            return builder
                    .contractNegotiationId("id")
                    .protocol("test")
                    .callbackAddresses(callbacks)
                    .counterPartyAddress("addr")
                    .counterPartyId("provider");
        }
    }

}
