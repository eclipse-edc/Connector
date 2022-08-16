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

package org.eclipse.dataspaceconnector.spi.event;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.eclipse.dataspaceconnector.spi.event.asset.AssetCreated;
import org.eclipse.dataspaceconnector.spi.event.asset.AssetDeleted;
import org.eclipse.dataspaceconnector.spi.event.contractdefinition.ContractDefinitionCreated;
import org.eclipse.dataspaceconnector.spi.event.contractdefinition.ContractDefinitionDeleted;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationApproved;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationConfirmed;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationDeclined;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationFailed;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationInitiated;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationOffered;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.dataspaceconnector.spi.event.policydefinition.PolicyDefinitionCreated;
import org.eclipse.dataspaceconnector.spi.event.policydefinition.PolicyDefinitionDeleted;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessCancelled;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessCompleted;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessDeprovisioned;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessEnded;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessFailed;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessInitiated;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessProvisioned;
import org.eclipse.dataspaceconnector.spi.event.transferprocess.TransferProcessRequested;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EventTest {

    private final TypeManager typeManager = new TypeManager();

    @ParameterizedTest
    @ArgumentsSource(EventInstances.class)
    void serdes(Event<?> event) {
        var eventClass = event.getClass();
        typeManager.registerTypes(new NamedType(eventClass, eventClass.getSimpleName()));

        var json = typeManager.writeValueAsString(event);
        var deserialized = typeManager.readValue(json, Event.class);

        assertThat(deserialized)
                .isInstanceOf(eventClass)
                .usingRecursiveComparison().isEqualTo(event);
    }

    private static class EventInstances implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var eventBuilders = Stream.of(
                    AssetCreated.Builder.newInstance().assetId("id"),
                    AssetDeleted.Builder.newInstance().assetId("id"),
                    ContractDefinitionCreated.Builder.newInstance().contractDefinitionId("id"),
                    ContractDefinitionDeleted.Builder.newInstance().contractDefinitionId("id"),
                    ContractNegotiationApproved.Builder.newInstance().contractNegotiationId("id"),
                    ContractNegotiationConfirmed.Builder.newInstance().contractNegotiationId("id"),
                    ContractNegotiationDeclined.Builder.newInstance().contractNegotiationId("id"),
                    ContractNegotiationFailed.Builder.newInstance().contractNegotiationId("id"),
                    ContractNegotiationInitiated.Builder.newInstance().contractNegotiationId("id"),
                    ContractNegotiationOffered.Builder.newInstance().contractNegotiationId("id"),
                    ContractNegotiationRequested.Builder.newInstance().contractNegotiationId("id"),
                    PolicyDefinitionCreated.Builder.newInstance().policyDefinitionId("id"),
                    PolicyDefinitionDeleted.Builder.newInstance().policyDefinitionId("id"),
                    TransferProcessCancelled.Builder.newInstance().transferProcessId("id"),
                    TransferProcessCompleted.Builder.newInstance().transferProcessId("id"),
                    TransferProcessDeprovisioned.Builder.newInstance().transferProcessId("id"),
                    TransferProcessEnded.Builder.newInstance().transferProcessId("id"),
                    TransferProcessFailed.Builder.newInstance().transferProcessId("id"),
                    TransferProcessInitiated.Builder.newInstance().transferProcessId("id"),
                    TransferProcessProvisioned.Builder.newInstance().transferProcessId("id"),
                    TransferProcessRequested.Builder.newInstance().transferProcessId("id")
            );

            return eventBuilders
                    .map(it -> it.id(UUID.randomUUID().toString()).at(Clock.systemUTC().millis()).build())
                    .map(Arguments::of);
        }
    }

}