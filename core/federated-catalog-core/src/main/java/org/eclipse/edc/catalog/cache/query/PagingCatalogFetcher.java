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

package org.eclipse.edc.catalog.cache.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.message.Range;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.edc.federatedcatalog.util.FederatedCatalogUtil.copy;
import static org.eclipse.edc.federatedcatalog.util.FederatedCatalogUtil.merge;

/**
 * Helper class that runs through a loop and sends {@link CatalogRequestMessage}s until no more {@link ContractOffer}s are
 * received. This is useful to avoid overloading the provider connector by chunking the resulting response payload
 * size.
 */
public class PagingCatalogFetcher {
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;
    private final SingleParticipantContextSupplier participantContextSupplier;
    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLdService;

    public PagingCatalogFetcher(RemoteMessageDispatcherRegistry dispatcherRegistry, SingleParticipantContextSupplier participantContextSupplier,
                                Monitor monitor, ObjectMapper objectMapper, TypeTransformerRegistry transformerRegistry, JsonLd jsonLdService) {
        this.dispatcherRegistry = dispatcherRegistry;
        this.participantContextSupplier = participantContextSupplier;
        this.monitor = monitor;
        this.objectMapper = objectMapper;
        this.transformerRegistry = transformerRegistry;
        this.jsonLdService = jsonLdService;
    }

    /**
     * Gets all contract offers. Requests are split in digestible chunks to match {@code batchSize} until no more offers
     * can be obtained.
     *
     * @param catalogRequest The catalog request. This will be copied for every request.
     * @param from           The (zero-based) index of the first item
     * @param batchSize      The size of one batch
     * @return A list of {@link ContractOffer} objects
     */
    public @NotNull CompletableFuture<Catalog> fetch(CatalogRequestMessage catalogRequest, int from, int batchSize) {

        var range = new Range(from, from + batchSize);
        var rangedRequest = CatalogRequestMessage.Builder.newInstance()
                .counterPartyAddress(catalogRequest.getCounterPartyAddress())
                .counterPartyId(catalogRequest.getCounterPartyId())
                .protocol(catalogRequest.getProtocol())
                .querySpec(QuerySpec.Builder.newInstance().range(range).build())
                .build();

        var participantResult  = participantContextSupplier.get().map(ParticipantContext::getParticipantContextId);
        if (participantResult.failed()) {
            return failedFuture(new EdcException(participantResult.getFailureDetail()));
        }

        return dispatcherRegistry.dispatch(participantResult.getContent(), byte[].class, rangedRequest)
                .thenCompose(this::readCatalogFrom)
                .thenApply(catalog -> copy(catalog).build())
                .thenCompose(catalog -> {

                    var datasets = catalog.getDatasets();
                    if (datasets.size() >= batchSize) {
                        monitor.debug(format("Fetching next batch from %s to %s", from, from + batchSize));
                        return fetch(rangedRequest, range.getFrom() + batchSize, batchSize)
                                .thenApply(catalogChunk -> merge(catalog, catalogChunk));
                    } else {
                        return completedFuture(catalog);
                    }
                });
    }

    private CompletableFuture<Catalog> readCatalogFrom(StatusResult<byte[]> bytes) {
        if (bytes.failed()) {
            return CompletableFuture.failedFuture(new EdcException(bytes.getFailureDetail()));
        }
        try {
            var json = new String(bytes.getContent());
            var catalogJsonObject = objectMapper.readValue(json, JsonObject.class);
            return jsonLdService.expand(catalogJsonObject)
                    .compose(expandedJson -> transformerRegistry.transform(expandedJson, Catalog.class))
                    .map(CompletableFuture::completedFuture)
                    .orElse((Failure f) -> failedFuture(new EdcException(f.getFailureDetail())));
        } catch (JsonProcessingException e) {
            monitor.severe(() -> "Error parsing Catalog from byes", e);
            return failedFuture(e);
        }
    }

}
