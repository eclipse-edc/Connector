/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.cache.query.PagingCatalogFetcher;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.message.Range;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.catalog.TestUtil.createCatalog;
import static org.eclipse.edc.catalog.TestUtil.registerTransformers;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class PagingCatalogFetcherTest {

    private final RemoteMessageDispatcherRegistry dispatcherRegistryMock = mock();
    private final ObjectMapper objectMapper = createObjectMapper();
    private final TitaniumJsonLd jsonLdService = new TitaniumJsonLd(mock());
    private final SingleParticipantContextSupplier participantContextSupplier = () -> ServiceResult.success(
            ParticipantContext.Builder.newInstance().participantContextId("participantContext").identity("identity").build());
    private final TypeTransformerRegistry typeTransformerRegistry = new TypeTransformerRegistryImpl();
    private PagingCatalogFetcher fetcher;

    @BeforeEach
    void setup() {
        registerTransformers(typeTransformerRegistry);

        fetcher = new PagingCatalogFetcher(dispatcherRegistryMock, participantContextSupplier, mock(), objectMapper, typeTransformerRegistry, jsonLdService);
    }

    @Test
    void fetchAll() throws JsonProcessingException {
        var cat1 = createCatalog(5);
        var cat2 = createCatalog(5);
        var cat3 = createCatalog(3);
        when(dispatcherRegistryMock.dispatch(any(), eq(byte[].class), any(CatalogRequestMessage.class)))
                .thenReturn(completedFuture(toBytes(cat1)))
                .thenReturn(completedFuture(toBytes(cat2)))
                .thenReturn(completedFuture(toBytes(cat3)))
                .thenReturn(completedFuture(toBytes(emptyCatalog())));

        var request = createRequest();

        var catalog = fetcher.fetch(request, 0, 5);
        assertThat(catalog).isCompletedWithValueMatching(list -> list.getDatasets().size() == 13 &&
                list.getDatasets().stream().allMatch(o -> o.getId().matches("(dataset-)\\d|1[0-3]")));


        var captor = forClass(CatalogRequestMessage.class);
        verify(dispatcherRegistryMock, times(3)).dispatch(any(), eq(byte[].class), captor.capture());

        // verify the sequence of requests
        assertThat(captor.getAllValues())
                .extracting(l -> l.getQuerySpec().getRange())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new Range(0, 5), new Range(5, 10), new Range(10, 15));
    }


    private StatusResult<byte[]> toBytes(Catalog catalog) throws JsonProcessingException {
        var jo = typeTransformerRegistry.transform(catalog, JsonObject.class).getContent();
        var expanded = jsonLdService.expand(jo).getContent();
        var expandedStr = objectMapper.writeValueAsString(expanded);
        return StatusResult.success(expandedStr.getBytes());
    }

    private CatalogRequestMessage createRequest() {
        return CatalogRequestMessage.Builder.newInstance()
                .counterPartyAddress("test-address")
                .protocol(DATASPACE_PROTOCOL_HTTP_V_2025_1)
                .build();
    }

    private Catalog emptyCatalog() {
        return Catalog.Builder.newInstance().id("id").participantId("test-participant").datasets(emptyList()).dataServices(emptyList()).build();
    }

}
