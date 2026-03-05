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

package org.eclipse.edc.catalog.cache.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.spi.model.CatalogUpdateResponse;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.crawler.spi.model.UpdateRequest;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.catalog.TestUtil.createCatalog;
import static org.eclipse.edc.catalog.TestUtil.registerTransformers;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DspCatalogRequestActionTest {

    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = new TypeTransformerRegistryImpl();
    private final TitaniumJsonLd jsonLdService = new TitaniumJsonLd(mock());
    private final ObjectMapper objectMapper = createObjectMapper();
    private final SingleParticipantContextSupplier participantContextSupplier = () -> ServiceResult.success(
            ParticipantContext.Builder.newInstance().participantContextId("participantContext").identity("identity").build());
    private final DspCatalogRequestAction action = new DspCatalogRequestAction(dispatcherRegistry, participantContextSupplier, mock(), objectMapper, typeTransformerRegistry, jsonLdService);

    @BeforeEach
    void setup() {
        registerTransformers(typeTransformerRegistry);
    }

    @Test
    void apply_withFlatCatalog() {
        var request = new UpdateRequest("test-node-id", "https://example.com/test-node-id", DATASPACE_PROTOCOL_HTTP_V_2025_1);

        var catalog = toBytes(createCatalog("test-catalog-id"));
        when(dispatcherRegistry.dispatch(any(), eq(byte[].class), any(CatalogRequestMessage.class)))
                .thenReturn(completedFuture(catalog));

        assertThat(action.apply(request)).isCompletedWithValueMatching(updateResponse -> {
            if (updateResponse instanceof CatalogUpdateResponse cr) {
                return cr.getCatalog() != null &&
                        cr.getCatalog().getId().equals("test-catalog-id") &&
                        cr.getSource().equals("https://example.com/test-node-id");
            }
            return false;
        });
    }

    @Test
    void apply_withOnlyNestedCatalog() {
        var request = new UpdateRequest("test-node-id", "https://example.com/test-node-id", DATASPACE_PROTOCOL_HTTP_V_2025_1);

        var rootCatalog = createCatalog("root-catalog-id");
        rootCatalog.getDatasets().clear();
        var nestedCatalog = createCatalog("nested-catalog-id");
        rootCatalog.getDatasets().add(nestedCatalog);

        when(dispatcherRegistry.dispatch(any(), eq(byte[].class), any(CatalogRequestMessage.class)))
                .thenReturn(completedFuture(toBytes(rootCatalog)))
                .thenReturn(completedFuture(toBytes(nestedCatalog)));

        assertThat(action.apply(request)).isCompletedWithValueMatching(updateResponse -> {
            if (updateResponse instanceof CatalogUpdateResponse cr) {
                return cr.getCatalog() != null &&
                        cr.getCatalog().getId().equals("root-catalog-id") &&
                        cr.getSource().equals("https://example.com/test-node-id") &&
                        cr.getCatalog().getDatasets().stream().allMatch(dataset -> dataset.getId().equals("nested-catalog-id"));
            }
            return false;
        }, "Contains nested catalog datasets");
    }

    @Test
    void apply_withRootDatasets_andNestedCatalog() {
        var request = new UpdateRequest("test-node-id", "https://example.com/test-node-id", DATASPACE_PROTOCOL_HTTP_V_2025_1);

        var rootCatalog = createCatalog("root-catalog-id");
        var nestedCatalog = createCatalog("nested-catalog-id");
        rootCatalog.getDatasets().add(nestedCatalog);

        when(dispatcherRegistry.dispatch(any(), eq(byte[].class), any(CatalogRequestMessage.class)))
                .thenReturn(completedFuture(toBytes(rootCatalog)))
                .thenReturn(completedFuture(toBytes(nestedCatalog)));

        assertThat(action.apply(request)).isCompletedWithValueMatching(updateResponse -> {
            if (updateResponse instanceof CatalogUpdateResponse cr) {
                return cr.getCatalog() != null &&
                        cr.getCatalog().getId().equals("root-catalog-id") &&
                        cr.getSource().equals("https://example.com/test-node-id") &&
                        cr.getCatalog().getDatasets().stream()
                                .allMatch(dataset -> dataset.getId().equals("nested-catalog-id") ||
                                        dataset.getId().equals("root-catalog-id-dataset"));
            }
            return false;
        }, "Contains root catalog datasets plus nested catalog datasets");
    }

    private StatusResult<byte[]> toBytes(Catalog catalog) {
        try {
            var jo = typeTransformerRegistry.transform(catalog, JsonObject.class).getContent();
            var expanded = jsonLdService.expand(jo).getContent();
            var expandedStr = objectMapper.writeValueAsString(expanded);
            return StatusResult.success(expandedStr.getBytes());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
