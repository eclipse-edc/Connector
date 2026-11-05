/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.crawler.cache.query;

import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolRemoteMessageDispatcher;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.federatedcatalog.util.FederatedCatalogUtil.copy;

/**
 * Fetches the catalog of a remote DSP node. Sub-catalogs are fetched recursively so that the returned {@link Catalog}
 * preserves the remote hierarchy.
 */
public class DspCatalogFetcher {
    private static final int INITIAL_OFFSET = 0;
    private static final int BATCH_SIZE = 100;
    private final PagingCatalogFetcher fetcher;

    public DspCatalogFetcher(ProtocolRemoteMessageDispatcher messageDispatcher,
                             Monitor monitor, ObjectMapper objectMapper, TypeTransformerRegistry transformerRegistry, JsonLd jsonLdService
    ) {
        fetcher = new PagingCatalogFetcher(messageDispatcher, monitor, objectMapper, transformerRegistry, jsonLdService);
    }

    /**
     * Fetches the (possibly paged) catalog from the given node and expands any nested sub-catalogs.
     *
     * @param participantContextId the local participant context on whose behalf the crawl is performed
     * @param nodeId               the id of the remote node
     * @param nodeUrl              the DSP endpoint of the remote node
     * @param protocol             the protocol to use
     */
    public CompletableFuture<Catalog> fetch(String participantContextId, String nodeId, String nodeUrl, String protocol) {
        var catalogRequest = CatalogRequestMessage.Builder.newInstance()
                .protocol(protocol)
                .counterPartyAddress(nodeUrl)
                .counterPartyId(nodeId)
                .build();

        return fetcher.fetch(participantContextId, catalogRequest, INITIAL_OFFSET, BATCH_SIZE)
                .thenCompose(rootCatalog -> expandCatalog(participantContextId, rootCatalog, protocol));
    }

    /**
     * This will go through the root catalog, and for every {@link Dataset}, that is in fact a {@link Catalog}, it will recurse down
     * and fetch that {@link Catalog} as well.
     * Note that this method will preserve hierarchy, so there could be catalogs within catalogs withing catalogs...
     *
     * @param participantContextId the local participant context on whose behalf the sub-catalogs are fetched
     * @param rootCatalog the root catalog, e.g. of a catalog server
     * @param protocol the protocol
     * @return a {@link Catalog} that contains expanded subcatalogs
     */
    private CompletableFuture<Catalog> expandCatalog(String participantContextId, Catalog rootCatalog, String protocol) {
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
                            return fetch(participantContextId, id, url, protocol); //recurse into the sub-catalog
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(CompletableFuture::join);

        var datasets = ofNullable(partitions.get(Dataset.class))
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
        expandedSubCatalogs.forEach(datasets::add);
        return completedFuture(copy(rootCatalog, datasets).build());

    }

}
