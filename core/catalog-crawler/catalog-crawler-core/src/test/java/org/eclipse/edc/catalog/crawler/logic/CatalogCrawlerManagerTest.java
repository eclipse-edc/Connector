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

package org.eclipse.edc.catalog.crawler.logic;

import org.eclipse.edc.catalog.crawler.cache.query.DspCatalogFetcher;
import org.eclipse.edc.catalog.spi.CatalogCrawlerConfiguration;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.crawler.spi.TargetNodeFilter;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CatalogCrawlerManagerTest {

    public static final String TEST_PROTOCOL = "test-protocol";
    private static final String PARTICIPANT_CONTEXT_ID = "participant-context-id";
    private final TargetNodeDirectory nodeDirectoryMock = mock();
    private final Monitor monitorMock = mock();
    private final DspCatalogFetcher catalogFetcher = mock();
    private final FederatedCatalogCache store = mock();
    private final ParticipantContextStore participantContextStore = mock();

    private CatalogCrawlerManager manager = createManagerBuilder().build();

    @BeforeEach
    void setUp() {
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(List.of(participantContext(PARTICIPANT_CONTEXT_ID))));
    }

    @Test
    void shouldStoreTheCatalog() {
        when(nodeDirectoryMock.getAll(PARTICIPANT_CONTEXT_ID)).thenReturn(List.of(createNode()));
        var catalog = new Catalog();
        when(catalogFetcher.fetch(any(), any(), any(), any())).thenReturn(completedFuture(catalog));

        manager.start();

        await().untilAsserted(() -> {
            verify(store).expireAll(PARTICIPANT_CONTEXT_ID);
            verify(store).deleteExpired(PARTICIPANT_CONTEXT_ID);

            verify(store).save(PARTICIPANT_CONTEXT_ID, catalog);
        });
    }

    @Test
    void shouldUseCounterPartyId_asCounterPartyIdentity() {
        var node = new TargetNode("testnode", "catalog-1", "did:web:identity", "http://test.com", PARTICIPANT_CONTEXT_ID, List.of(TEST_PROTOCOL));
        when(nodeDirectoryMock.getAll(PARTICIPANT_CONTEXT_ID)).thenReturn(List.of(node));
        when(catalogFetcher.fetch(any(), any(), any(), any())).thenReturn(completedFuture(new Catalog()));

        manager.start();

        await().untilAsserted(() ->
                verify(catalogFetcher).fetch(PARTICIPANT_CONTEXT_ID, "did:web:identity", "http://test.com", TEST_PROTOCOL));
    }

    @Test
    void shouldCrawlOnBehalfOfEachParticipantContext() {
        var otherParticipant = "other-participant-context-id";
        when(participantContextStore.query(any())).thenReturn(StoreResult.success(
                List.of(participantContext(PARTICIPANT_CONTEXT_ID), participantContext(otherParticipant))));
        when(nodeDirectoryMock.getAll(PARTICIPANT_CONTEXT_ID)).thenReturn(List.of(createNode()));
        when(nodeDirectoryMock.getAll(otherParticipant)).thenReturn(List.of(
                new TargetNode("othernode", "did:web:" + UUID.randomUUID(), "http://other.com", otherParticipant, List.of(TEST_PROTOCOL))));
        when(catalogFetcher.fetch(any(), any(), any(), any())).thenReturn(completedFuture(new Catalog()));

        manager.start();

        await().untilAsserted(() -> {
            verify(store).expireAll(PARTICIPANT_CONTEXT_ID);
            verify(store).expireAll(otherParticipant);
            verify(store).save(eq(PARTICIPANT_CONTEXT_ID), any());
            verify(store).save(eq(otherParticipant), any());
        });
    }

    @Test
    void shouldSkipNode_whenNoProtocolDeclared() {
        when(nodeDirectoryMock.getAll(PARTICIPANT_CONTEXT_ID)).thenReturn(List.of(
                new TargetNode("testnode", "did:web:" + UUID.randomUUID(), "http://test.com", PARTICIPANT_CONTEXT_ID, List.of())));

        manager.start();

        await().untilAsserted(() -> verify(monitorMock).warning(startsWith("Node")));
        verifyNoInteractions(catalogFetcher);
    }

    @Test
    void shouldLogSevere_whenUnexpectedException() {
        when(nodeDirectoryMock.getAll(PARTICIPANT_CONTEXT_ID)).thenReturn(List.of(createNode()));
        var exc = new EdcException("unexpected exception");
        when(catalogFetcher.fetch(any(), any(), any(), any())).thenReturn(failedFuture(exc));

        manager.start();

        await().untilAsserted(() -> {
            verify(monitorMock, atLeastOnce()).severe(startsWith("Unexpected exception occurred while crawling"), isA(CompletionException.class));
        });
    }

    @Test
    void shouldLogInfo_whenQueryReturnsConnectException() {
        when(nodeDirectoryMock.getAll(PARTICIPANT_CONTEXT_ID)).thenReturn(List.of(createNode()));
        when(catalogFetcher.fetch(any(), any(), any(), any())).thenReturn(failedFuture(new ConnectException("cannot connect")));

        manager.start();

        await().untilAsserted(() -> {
            verify(monitorMock, atLeastOnce()).info(startsWith("Cannot connect to node"));
        });
    }

    @Test
    void shouldDiscardNode_whenMaxRetriesExceeded() {
        var maxRetries = 2;
        // retry immediately, and use a long period so the next crawl cycle cannot interfere with the count
        var manager = createManagerBuilder()
                .configuration(new CatalogCrawlerConfiguration(true, 1, 3600, 0, maxRetries, 0))
                .build();
        when(nodeDirectoryMock.getAll(PARTICIPANT_CONTEXT_ID)).thenReturn(List.of(createNode()));
        when(catalogFetcher.fetch(any(), any(), any(), any())).thenReturn(failedFuture(new EdcException("unexpected exception")));

        manager.start();

        await().untilAsserted(() -> verify(monitorMock).severe(startsWith("The following Node has errored out more than")));
        // one initial attempt plus maxRetries, then the node is discarded
        verify(catalogFetcher, times(maxRetries + 1)).fetch(any(), any(), any(), any());
    }

    @Test
    void executePlan_withCustomFiltering() {
        when(nodeDirectoryMock.getAll(PARTICIPANT_CONTEXT_ID)).thenReturn(List.of(createNode(), createNode(), createNode()));
        var filter = mock(TargetNodeFilter.class);
        manager = createManagerBuilder().nodeFilterFunction(filter).build();

        manager.start();

        await().untilAsserted(() -> {
            verify(filter, times(3)).test(any());
        });
    }

    @Test
    void executePlan_shouldNotRunPlanWhenGloballyDisabled() {
        manager = createManagerBuilder().configuration(disabledCrawler()).build();

        manager.start();

        verify(monitorMock).warning(anyString());
        verifyNoInteractions(catalogFetcher);
    }

    @Test
    void stop_shouldNotStopPlanWhenGloballyDisabled() {

        manager = createManagerBuilder().configuration(disabledCrawler()).build();
        manager.stop();

        verify(monitorMock).warning(anyString());
    }

    @Test
    void stop_shouldStopPlanWhenGloballyEnabled() {
        manager = createManagerBuilder().configuration(enabledCrawler()).build();
        manager.stop();

        verifyNoMoreInteractions(monitorMock);
    }

    private CatalogCrawlerConfiguration disabledCrawler() {
        return new CatalogCrawlerConfiguration(false, 5, 5, 5, 5, 5);
    }

    private CatalogCrawlerConfiguration enabledCrawler() {
        return new CatalogCrawlerConfiguration(true, 5, 20, 0, 5, 5);
    }

    @NotNull
    private CatalogCrawlerManager.Builder createManagerBuilder() {
        return CatalogCrawlerManager.Builder.newInstance()
                .configuration(enabledCrawler())
                .nodeDirectory(nodeDirectoryMock)
                .catalogFetcher(catalogFetcher)
                .store(store)
                .participantContextStore(participantContextStore)
                .monitor(monitorMock);
    }

    @NotNull
    private TargetNode createNode() {
        return new TargetNode("testnode" + UUID.randomUUID(), "did:web:" + UUID.randomUUID(), "http://test.com", PARTICIPANT_CONTEXT_ID, List.of(TEST_PROTOCOL));
    }

    @NotNull
    private ParticipantContext participantContext(String id) {
        return ParticipantContext.Builder.newInstance().id(id).identity("did:web:" + id).state(ParticipantContextState.ACTIVATED).build();
    }

}
