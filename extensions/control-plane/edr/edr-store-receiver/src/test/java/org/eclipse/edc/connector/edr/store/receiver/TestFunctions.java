/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.edr.store.receiver;

import org.eclipse.edc.connector.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TestFunctions {

    @SuppressWarnings("unchecked")
    public static <T extends Event> EventEnvelope<T> envelopeFor(T event) {

        return EventEnvelope.Builder
                .newInstance()
                .id(UUID.randomUUID().toString())
                .at(System.currentTimeMillis())
                .payload(event)
                .build();
    }

    public static <T extends TransferProcessEvent, B extends TransferProcessEvent.Builder<T, B>> B baseBuilder(B builder) {
        var callbacks = List.of(CallbackAddress.Builder.newInstance().uri("http://local").events(Set.of("test")).build());
        return builder.transferProcessId("id")
                .assetId("assetId")
                .type(TransferProcess.Type.CONSUMER.name())
                .contractId("agreementId")
                .callbackAddresses(callbacks);
    }

}
