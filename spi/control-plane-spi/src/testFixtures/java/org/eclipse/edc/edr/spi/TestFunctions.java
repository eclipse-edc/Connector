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

package org.eclipse.edc.edr.spi;


import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.UUID;

public class TestFunctions {

    public static EndpointDataReferenceEntry edrEntry(String assetId, String agreementId, String transferProcessId, String contractNegotiationId, String providerId) {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(assetId)
                .agreementId(agreementId)
                .transferProcessId(transferProcessId)
                .providerId(UUID.randomUUID().toString())
                .contractNegotiationId(contractNegotiationId)
                .participantContextId("participantContextId")
                .build();
    }

    public static EndpointDataReferenceEntry edrEntry(String assetId, String agreementId, String transferProcessId, String contractNegotiationId) {
        return edrEntry(assetId, agreementId, transferProcessId, contractNegotiationId, UUID.randomUUID().toString());
    }

    public static EndpointDataReferenceEntry.Builder edrEntryBuilder(String assetId, String agreementId, String transferProcessId, String contractNegotiationId, String providerId) {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(assetId)
                .agreementId(agreementId)
                .transferProcessId(transferProcessId)
                .providerId(UUID.randomUUID().toString())
                .contractNegotiationId(contractNegotiationId)
                .participantContextId("participantContextId");
    }

    public static EndpointDataReferenceEntry.Builder edrEntryBuilder(String assetId, String agreementId, String transferProcessId, String contractNegotiationId) {
        return edrEntryBuilder(assetId, agreementId, transferProcessId, contractNegotiationId, UUID.randomUUID().toString());
    }

    public static EndpointDataReferenceEntry.Builder edrEntryBuilder() {
        return edrEntryBuilder("assetId", "agreementId", "transferProcessId", "contractNegotiationId");
    }

    public static EndpointDataReferenceEntry edrEntry() {
        return edrEntry("assetId", "agreementId", "transferProcessId", "contractNegotiationId");
    }

    public static DataAddress dataAddress() {
        return DataAddress.Builder.newInstance().type("test").build();
    }

}
