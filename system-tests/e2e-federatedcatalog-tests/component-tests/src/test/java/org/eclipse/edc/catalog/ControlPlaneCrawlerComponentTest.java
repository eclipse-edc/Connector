/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Contributors to the Eclipse Foundation - initial API and implementation
 *
 */

package org.eclipse.edc.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolRemoteMessageDispatcher;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.test.TestJsonLd;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.catalog.TestFunctions.catalogBuilder;
import static org.eclipse.edc.catalog.TestFunctions.catalogOf;
import static org.eclipse.edc.catalog.TestFunctions.createDataset;
import static org.eclipse.edc.catalog.TestFunctions.emptyCatalog;
import static org.eclipse.edc.catalog.TestFunctions.queryCatalogApi;
import static org.eclipse.edc.catalog.TestFunctions.randomCatalog;
import static org.eclipse.edc.catalog.matchers.CatalogRequestMatcher.sentTo;
import static org.eclipse.edc.catalog.spi.CatalogConstants.PROPERTY_ORIGINATOR;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_TRANSFORMER_CONTEXT_V_2025_1;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Component test that validates the catalog crawler running inside a standard
 * control plane runtime (controlplane-base-bom), as opposed to a standalone federated
 * catalog runtime (federatedcatalog-base-bom).
 */
@ComponentTest
public class ControlPlaneCrawlerComponentTest {

    private static final Duration TEST_TIMEOUT = ofSeconds(10);
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    @RegisterExtension
    protected static RuntimeExtension runtime = new RuntimePerMethodExtension(
            new EmbeddedRuntime("controlplane-crawler",
                    ":dist:bom:controlplane-base-bom",
                    ":extensions:common:iam:iam-mock")
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                            Map.entry("edc.catalog.cache.execution.period.seconds", "2"),
                            Map.entry("edc.catalog.cache.partition.num.crawlers", "10"),
                            Map.entry("edc.catalog.cache.execution.delay.seconds", "1"),
                            Map.entry("web.http.management.port", valueOf(TestFunctions.MANAGEMENT_PORT)),
                            Map.entry("web.http.management.path", TestFunctions.MANAGEMENT_BASE_PATH),
                            Map.entry("web.http.port", valueOf(getFreePort())),
                            Map.entry("web.http.path", "/api"),
                            Map.entry("web.http.protocol.port", valueOf(getFreePort())),
                            Map.entry("web.http.protocol.path", "/api/v1/dsp"),
                            Map.entry("edc.participant.id", "test-participant")
                    )))
    );

    private final ProtocolRemoteMessageDispatcher dispatcher = mock();

    @BeforeEach
    void setUp(RuntimeExtension extension) {
        extension.registerServiceMock(ProtocolRemoteMessageDispatcher.class, dispatcher);
    }

    @Test
    @DisplayName("Verify crawler inside control plane: crawl single target, no results")
    void crawlSingle_noResults(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        directory.insert(targetNode());
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(emptyCatalog(toBytes(ttr)));

        await().untilAsserted(() -> {
            var response = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(response).hasSize(1);
            assertThat(response).allSatisfy(c -> assertThat(c.getDatasets()).isNullOrEmpty());
        });
    }

    @Test
    @DisplayName("Verify crawler inside control plane: crawl single target, yields results")
    void crawlSingle_withResults(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        directory.insert(targetNode());
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(randomCatalog(toBytes(ttr), "test-catalog-id", 5))
                .thenReturn(emptyCatalog(toBytes(ttr))); // this is important, otherwise there is an endless loop!

        await().untilAsserted(() -> {
            var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(catalogs).allSatisfy(c -> assertThat(c.getDatasets()).hasSize(5));
        });
    }

    @Test
    @DisplayName("Crawl a single target, returns a catalog of catalogs")
    void crawlSingle_withCatalogOfCatalogs(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(randomCatalog(catalog -> StatusResult.success(TestUtils.getResourceFileContentAsString("catalog_of_catalogs.json").getBytes()), "root-catalog-id", 5))
                .thenReturn(randomCatalog(catalog -> StatusResult.success(TestUtils.getResourceFileContentAsString("catalog.json").getBytes()), "sub-catalog-id", 5));

        await().untilAsserted(() -> {
            var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(catalogs).isNotEmpty().allSatisfy(c -> {
                assertThat(c.getDatasets()).hasSize(2);
                assertThat(c.getDatasets()).anySatisfy(ds -> assertThat(ds).isInstanceOf(Catalog.class));
            });
        });
    }

    @Test
    @DisplayName("Verify crawler inside control plane: crawl single target, correct originator")
    void crawlSingle_verifyOriginator(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(randomCatalog(toBytes(ttr), "test-catalog-id", 3))
                .thenReturn(emptyCatalog(toBytes(ttr)));
        directory.insert(targetNode());

        await().untilAsserted(() -> {
            var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(catalogs).hasSize(1);
            assertThat(catalogs.get(0).getDatasets()).hasSize(3);
            assertThat(catalogs).extracting(Catalog::getProperties)
                    .allSatisfy(a -> assertThat(a).containsEntry(PROPERTY_ORIGINATOR, "http://test-node.com"));
        });
    }

    @Test
    @DisplayName("Crawl a single targets, > 100 results, needs paging")
    void crawlSingle_withPagedResults(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(randomCatalog(toBytes(ttr), "test-catalog-id", 100))
                .thenReturn(randomCatalog(toBytes(ttr), "test-catalog-id", 100))
                .thenReturn(randomCatalog(toBytes(ttr), "test-catalog-id", 50));

        await().untilAsserted(() -> {
            var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(catalogs.size()).isEqualTo(1);
            assertThat(catalogs.get(0).getDatasets()).hasSize(250);
        });
        verify(dispatcher, atLeast(3)).dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class));

    }

    @Test
    @DisplayName("Verify crawler inside control plane: crawl multiple targets with distinct catalogs")
    void crawlMultiple_shouldCollectAll(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        var node1 = new TargetNode("test-node1", "did:web:" + UUID.randomUUID(), "http://test-node1.com", singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));
        var node2 = new TargetNode("test-node2", "did:web:" + UUID.randomUUID(), "http://test-node2.com", singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));
        directory.insert(node1);
        directory.insert(node2);

        when(dispatcher.dispatch(any(), eq(byte[].class), argThat(sentTo(node1.id(), node1.targetUrl()))))
                .thenReturn(catalogOf(toBytes(ttr), "catalog-node1", createDataset("offer1"), createDataset("offer2")))
                .thenReturn(emptyCatalog(toBytes(ttr)));
        when(dispatcher.dispatch(any(), eq(byte[].class), argThat(sentTo(node2.id(), node2.targetUrl()))))
                .thenReturn(catalogOf(toBytes(ttr), "catalog-node2", createDataset("offer3")))
                .thenReturn(emptyCatalog(toBytes(ttr)));

        await().untilAsserted(() -> {
            var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(catalogs).hasSize(2);
            assertThat(catalogs.stream().flatMap(c -> c.getDatasets().stream()).map(Dataset::getId))
                    .containsExactlyInAnyOrder("offer1", "offer2", "offer3");
        });
    }

    @Test
    @DisplayName("Verify crawler inside control plane: crawl with asset deletions")
    void crawlSingle_withDeletions(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        directory.insert(targetNode());

        var catalogId = "test-catalog-id";
        StatusResult<byte[]> result;
        Catalog catalog = catalogBuilder().id(catalogId).datasets(new ArrayList<>(List.of(
                createDataset("offer1"), createDataset("offer2")
        ))).build();
        try {
            var dspTransformerRegistry = ttr.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
            var jo = dspTransformerRegistry.transform(catalog, JsonObject.class).orElseThrow(AssertionError::new);
            var expanded = TestJsonLd.expand(jo);
            var expandedStr = OBJECT_MAPPER.writeValueAsString(expanded);
            result = StatusResult.success(expandedStr.getBytes());
        } catch (JsonProcessingException ex) {
            throw new AssertionError(ex);
        }
        StatusResult<byte[]> result1;
        Catalog catalog1 = catalogBuilder().id(catalogId).datasets(new ArrayList<>(List.of(
                createDataset("offer1"), createDataset("offer2"), createDataset("offer3")
        ))).build();
        try {
            var dspTransformerRegistry = ttr.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
            var jo = dspTransformerRegistry.transform(catalog1, JsonObject.class).orElseThrow(AssertionError::new);
            var expanded = TestJsonLd.expand(jo);
            var expandedStr = OBJECT_MAPPER.writeValueAsString(expanded);
            result1 = StatusResult.success(expandedStr.getBytes());
        } catch (JsonProcessingException ex) {
            throw new AssertionError(ex);
        }
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(completedFuture(result1))
                .thenReturn(emptyCatalog(toBytes(ttr), catalogId))
                .thenReturn(completedFuture(result));

        await().untilAsserted(() -> {
            var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(catalogs).hasSize(1);
            assertThat(catalogs.get(0).getDatasets()).hasSize(2)
                    .noneMatch(offer -> offer.getId().equals("offer3"));
        });
    }

    @Test
    @DisplayName("Crawl a single target twice, emulate deletion of assets")
    void crawlSingle_withDeletions_shouldRemove(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());
        StatusResult<byte[]> result;
        Catalog catalog = catalogBuilder().id("test-catalog-id").datasets(new ArrayList<>(List.of(
                createDataset("offer1"), createDataset("offer2")/* this one is "deleted": createDataset("offer3") */
        ))).build();
        try {
            var dspTransformerRegistry = ttr.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
            var jo = dspTransformerRegistry.transform(catalog, JsonObject.class).orElseThrow(AssertionError::new);
            var expanded = TestJsonLd.expand(jo);
            var expandedStr = OBJECT_MAPPER.writeValueAsString(expanded);
            result = StatusResult.success(expandedStr.getBytes());
        } catch (JsonProcessingException ex) {
            throw new AssertionError(ex);
        }
        /* this one is "deleted": createDataset("offer3") */
        StatusResult<byte[]> result1;
        Catalog catalog1 = catalogBuilder().id("test-catalog-id").datasets(new ArrayList<>(List.of(
                createDataset("offer1"), createDataset("offer2"), createDataset("offer3")
        ))).build();
        try {
            var dspTransformerRegistry = ttr.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
            var jo = dspTransformerRegistry.transform(catalog1, JsonObject.class).orElseThrow(AssertionError::new);
            var expanded = TestJsonLd.expand(jo);
            var expandedStr = OBJECT_MAPPER.writeValueAsString(expanded);
            result1 = StatusResult.success(expandedStr.getBytes());
        } catch (JsonProcessingException ex) {
            throw new AssertionError(ex);
        }
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(completedFuture(result1))
                .thenReturn(emptyCatalog(toBytes(ttr), "test-catalog-id"))
                .thenReturn(completedFuture(result));

        await().untilAsserted(() -> {
            var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(catalogs).hasSize(1);
            assertThat(catalogs.get(0).getDatasets()).hasSize(2)
                    .noneMatch(offer -> offer.getId().equals("offer3"));
            verify(dispatcher, atLeast(4)).dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class));
        });

    }

    @Test
    @DisplayName("Crawl a single target twice, emulate deleting and re-adding of assets with same ID")
    void crawlSingle_withUpdates_shouldReplace(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());
        StatusResult<byte[]> result;
        Catalog catalog = catalogBuilder().id("test-catalog-id").datasets(new ArrayList<>(List.of(
                createDataset("offer1"), createDataset("offer2"), createDataset("offer3")
        ))).build();
        try {
            var dspTransformerRegistry = ttr.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
            var jo = dspTransformerRegistry.transform(catalog, JsonObject.class).orElseThrow(AssertionError::new);
            var expanded = TestJsonLd.expand(jo);
            var expandedStr = OBJECT_MAPPER.writeValueAsString(expanded);
            result = StatusResult.success(expandedStr.getBytes());
        } catch (JsonProcessingException ex) {
            throw new AssertionError(ex);
        }
        StatusResult<byte[]> result1;
        Catalog catalog1 = catalogBuilder().id("test-catalog-id").datasets(new ArrayList<>(List.of(
                createDataset("offer1"), createDataset("offer2"), createDataset("offer3")
        ))).build();
        try {
            var dspTransformerRegistry = ttr.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
            var jo = dspTransformerRegistry.transform(catalog1, JsonObject.class).orElseThrow(AssertionError::new);
            var expanded = TestJsonLd.expand(jo);
            var expandedStr = OBJECT_MAPPER.writeValueAsString(expanded);
            result1 = StatusResult.success(expandedStr.getBytes());
        } catch (JsonProcessingException ex) {
            throw new AssertionError(ex);
        }
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(completedFuture(result1))
                .thenReturn(emptyCatalog(toBytes(ttr), "test-catalog-id"))
                .thenReturn(completedFuture(result));

        await().untilAsserted(() -> {
            var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(catalogs).hasSize(1);
            assertThat(catalogs.get(0).getDatasets()).hasSize(3);
            verify(dispatcher, atLeast(4)).dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class));
        });

    }

    @Test
    @DisplayName("Crawl a single target twice, emulate addition of assets")
    void crawlSingle_withAdditions_shouldAdd(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenAnswer(a -> {
                    StatusResult<byte[]> result;
                    Catalog catalog = catalogBuilder().id("test-cat")
                            .datasets(List.of(createDataset("dataset1"), createDataset("dataset2"))).build();
                    try {
                        var dspTransformerRegistry = ttr.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
                        var jo = dspTransformerRegistry.transform(catalog, JsonObject.class).orElseThrow(AssertionError::new);
                        var expanded = TestJsonLd.expand(jo);
                        var expandedStr = OBJECT_MAPPER.writeValueAsString(expanded);
                        result = StatusResult.success(expandedStr.getBytes());
                    } catch (JsonProcessingException ex) {
                        throw new AssertionError(ex);
                    }
                    return completedFuture(result);
                })
                .thenAnswer(a -> {
                    StatusResult<byte[]> result;
                    Catalog catalog = catalogBuilder().id("test-cat")
                            .datasets(List.of(createDataset("dataset1"), createDataset("dataset2"),
                                    createDataset("dataset3"), createDataset("dataset4"))).build();
                    try {
                        var dspTransformerRegistry = ttr.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
                        var jo = dspTransformerRegistry.transform(catalog, JsonObject.class).orElseThrow(AssertionError::new);
                        var expanded = TestJsonLd.expand(jo);
                        var expandedStr = OBJECT_MAPPER.writeValueAsString(expanded);
                        result = StatusResult.success(expandedStr.getBytes());
                    } catch (JsonProcessingException ex) {
                        throw new AssertionError(ex);
                    }
                    return completedFuture(result);
                });

        await().untilAsserted(() -> {
            var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(catalogs).hasSize(1);
            assertThat(catalogs)
                    .allSatisfy(cat -> assertThat(cat.getDatasets()).hasSize(4))
                    .allSatisfy(co -> assertThat(co.getDatasets().stream().map(Dataset::getId).map(id -> id.replace("dataset", "")))
                            .containsExactlyInAnyOrder("1", "2", "3", "4"));
            verify(dispatcher, atLeast(2)).dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class));
        });

    }

    @Test
    @DisplayName("Crawl a single target, verify that the originator information is properly inserted")
    void crawlSingle_verifyCorrectOriginator(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(randomCatalog(toBytes(ttr), "test-catalog-id", 5))
                .thenReturn(emptyCatalog(toBytes(ttr))); // this is important, otherwise there is an endless loop!

        await().untilAsserted(() -> {
            var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(catalogs).hasSize(1);
            assertThat(catalogs.get(0).getDatasets()).hasSize(5);
            assertThat(catalogs).extracting(Catalog::getProperties).allSatisfy(a -> assertThat(a).containsEntry(PROPERTY_ORIGINATOR, "http://test-node.com"));
        });
    }

    @Test
    @DisplayName("Crawl multiple targets, verify that all offers are collected")
    void crawlMany_shouldCollectAll(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        when(dispatcher.dispatch(any(), eq(byte[].class), any())).thenReturn(emptyCatalog(toBytes(ttr)));
        var numTotalAssets = new AtomicInteger();
        var rnd = new SecureRandom();
        var numTargets = 10;
        range(0, numTargets)
                .forEach(i -> {
                    var nodeUrl = format("http://test-node%s.com", i);
                    var numAssets = 1 + rnd.nextInt(10);
                    var nodeId = "did:web:" + i + "-count-" + numAssets;
                    var node = new TargetNode("test-node-" + i, nodeId, nodeUrl, singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));
                    when(dispatcher.dispatch(any(), eq(byte[].class), argThat(sentTo(nodeId, nodeUrl))))
                            .thenReturn(randomCatalog(toBytes(ttr), "catalog-" + nodeUrl, numAssets));

                    directory.insert(node);
                    numTotalAssets.addAndGet(numAssets);
                });

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
                    assertThat(catalogs).hasSize(numTargets);
                    //assert that the total number of offers across all catalogs is corrects
                    assertThat(catalogs.stream().mapToLong(c -> c.getDatasets().size()).sum()).isEqualTo(numTotalAssets.get());
                });
    }

    @Test
    @DisplayName("Crawl multiple targets with conflicting asset IDs")
    void crawlMultiple_whenConflictingAssetIds_shouldOverwrite(TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        var node1 = new TargetNode("test-node1", "did:web:" + UUID.randomUUID(), "http://test-node1.com", singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));
        var node2 = new TargetNode("test-node2", "did:web:" + UUID.randomUUID(), "http://test-node2.com", singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));

        directory.insert(node1);
        directory.insert(node2);

        when(dispatcher.dispatch(any(), eq(byte[].class), argThat(sentTo(node1.id(), node1.targetUrl()))))
                .thenReturn(catalogOf(toBytes(ttr), "catalog-" + node1.targetUrl(), createDataset("offer1"), createDataset("offer2"), createDataset("offer3")))
                .thenReturn(emptyCatalog(toBytes(ttr)));

        when(dispatcher.dispatch(any(), eq(byte[].class), argThat(sentTo(node2.id(), node2.targetUrl()))))
                .thenReturn(catalogOf(toBytes(ttr), "catalog-" + node2.targetUrl(), createDataset("offer14"), createDataset("offer32"), /*this one is conflicting:*/createDataset("offer3")))
                .thenReturn(emptyCatalog(toBytes(ttr)));

        await().untilAsserted(() -> {
            var catalogs = queryCatalogApi(jsonLd, jsonObject -> toCatalog(ttr, jsonObject));
            assertThat(catalogs).hasSize(2);
            assertThat(catalogs).anySatisfy(c -> assertThat(c.getProperties().get(PROPERTY_ORIGINATOR).toString()).startsWith("http://test-node1.com"));
            assertThat(catalogs).anySatisfy(c -> assertThat(c.getProperties().get(PROPERTY_ORIGINATOR).toString()).startsWith("http://test-node2.com"));
            assertThat(catalogs.stream().mapToLong(c -> c.getDatasets().size()).sum()).isEqualTo(6);
            assertThat(catalogs.stream().flatMap(c -> c.getDatasets().stream()).map(Dataset::getId))
                    .containsExactlyInAnyOrder("offer1", "offer2", "offer3", "offer14", "offer32", "offer3");
        });
    }


    private @NonNull Function<Catalog, StatusResult<byte[]>> toBytes(TypeTransformerRegistry typeTransformerRegistry) {
        return catalog -> {
            try {
                var dspTransformerRegistry = typeTransformerRegistry.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);
                var jo = dspTransformerRegistry.transform(catalog, JsonObject.class).orElseThrow(AssertionError::new);
                var expanded = TestJsonLd.expand(jo);
                var expandedStr = OBJECT_MAPPER.writeValueAsString(expanded);
                return StatusResult.success(expandedStr.getBytes());
            } catch (JsonProcessingException ex) {
                throw new AssertionError(ex);
            }
        };
    }

    private Catalog toCatalog(TypeTransformerRegistry ttr, JsonObject jsonObject) {
        return ttr.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1)
                .transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new);
    }

    private @NotNull TargetNode targetNode() {
        return new TargetNode("test-node", "did:web:" + UUID.randomUUID(), "http://test-node.com", singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));
    }
}
