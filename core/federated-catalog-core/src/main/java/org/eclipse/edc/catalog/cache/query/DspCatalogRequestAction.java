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

package org.eclipse.edc.catalog.cache.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.catalog.spi.model.CatalogUpdateResponse;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.crawler.spi.CrawlerAction;
import org.eclipse.edc.crawler.spi.model.UpdateRequest;
import org.eclipse.edc.crawler.spi.model.UpdateResponse;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.federatedcatalog.util.FederatedCatalogUtil.copy;

public class DspCatalogRequestAction implements CrawlerAction {
    private static final int INITIAL_OFFSET = 0;
    private static final int BATCH_SIZE = 100;
    private final PagingCatalogFetcher fetcher;

    public DspCatalogRequestAction(RemoteMessageDispatcherRegistry dispatcherRegistry, SingleParticipantContextSupplier participantContextSupplier,
                                   Monitor monitor, ObjectMapper objectMapper, TypeTransformerRegistry transformerRegistry, JsonLd jsonLdService
    ) {
        fetcher = new PagingCatalogFetcher(dispatcherRegistry, participantContextSupplier, monitor,  objectMapper, transformerRegistry, jsonLdService);
    }

    @Override
    public CompletableFuture<UpdateResponse> apply(UpdateRequest request) {
        var catalogRequest = CatalogRequestMessage.Builder.newInstance()
                .protocol(request.protocol())
                .counterPartyAddress(request.nodeUrl())
                .counterPartyId(request.nodeId())
                .build();

        var catalogFuture = fetcher.fetch(catalogRequest, INITIAL_OFFSET, BATCH_SIZE);

        return catalogFuture
                .thenCompose(rootCatalog -> expandCatalog(rootCatalog, request.protocol()))
                .thenApply(catalog -> new CatalogUpdateResponse(request.nodeUrl(), catalog));
    }

    /**
     * This will go through the root catalog, and for every {@link Dataset}, that is in fact a {@link Catalog}, it will recurse down
     * and fetch that {@link Catalog} as well.
     * Note that this method will preserve hierarchy, so there could be catalogs within catalogs withing catalogs...
     *
     * @param rootCatalog the root catalog, e.g. of a catalog server
     * @param protocol the protocol
     * @return a {@link Catalog} that contains expanded subcatalogs
     */
    private CompletableFuture<Catalog> expandCatalog(Catalog rootCatalog, String protocol) {
        var partitions = rootCatalog.getDatasets().stream().collect(Collectors.groupingBy(Dataset::getClass));

        var subCatalogs = partitions.get(Catalog.class);

        if (subCatalogs == null || subCatalogs.isEmpty()) {
            return completedFuture(rootCatalog);
        }

        var expandedSubCatalogs = subCatalogs.stream()
                .map(ds -> (Catalog) ds)
                .map(subCatalog -> subCatalog.getDataServices().stream()
                        .map(DataService::getEndpointUrl)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(url -> {
                            var id = ofNullable(subCatalog.getParticipantId()).orElseGet(rootCatalog::getParticipantId);
                            return new UpdateRequest(id, url, protocol);
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(this) //recursively call this.apply()
                .map(CompletableFuture::join)
                .map(ur -> (CatalogUpdateResponse) ur)
                .map(CatalogUpdateResponse::getCatalog);

        var datasets = ofNullable(partitions.get(Dataset.class))
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
        expandedSubCatalogs.forEach(datasets::add);
        return completedFuture(copy(rootCatalog, datasets).build());

    }

}
