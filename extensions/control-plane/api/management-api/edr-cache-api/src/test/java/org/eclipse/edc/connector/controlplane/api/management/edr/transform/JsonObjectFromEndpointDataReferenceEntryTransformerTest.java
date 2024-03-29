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

package org.eclipse.edc.connector.controlplane.api.management.edr.transform;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.controlplane.api.management.edr.transform.JsonObjectFromEndpointDataReferenceEntryTransformer;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CONTRACT_NEGOTIATION_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CREATED_AT;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_PROVIDER_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class JsonObjectFromEndpointDataReferenceEntryTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());

    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromEndpointDataReferenceEntryTransformer transformer = new JsonObjectFromEndpointDataReferenceEntryTransformer(jsonFactory);

    @Test
    void transform() {

        var entry = EndpointDataReferenceEntry.Builder.newInstance()
                .transferProcessId("transferProcessId")
                .assetId("assetId")
                .providerId("providerId")
                .agreementId("agreementId")
                .contractNegotiationId("contractNegotiationId")
                .build();

        var result = transformer.transform(entry, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("transferProcessId");
        assertThat(result.getString(TYPE)).isEqualTo(EDR_ENTRY_TYPE);
        assertThat(result.getString(EDR_ENTRY_CONTRACT_NEGOTIATION_ID)).isEqualTo("contractNegotiationId");
        assertThat(result.getString(EDR_ENTRY_TRANSFER_PROCESS_ID)).isEqualTo("transferProcessId");
        assertThat(result.getString(EDR_ENTRY_ASSET_ID)).isEqualTo("assetId");
        assertThat(result.getString(EDR_ENTRY_PROVIDER_ID)).isEqualTo("providerId");
        assertThat(result.getString(EDR_ENTRY_AGREEMENT_ID)).isEqualTo("agreementId");
        assertThat(result.getJsonNumber(EDR_ENTRY_CREATED_AT)).isNotNull();
        verify(context, never()).reportProblem(anyString());
    }
}
