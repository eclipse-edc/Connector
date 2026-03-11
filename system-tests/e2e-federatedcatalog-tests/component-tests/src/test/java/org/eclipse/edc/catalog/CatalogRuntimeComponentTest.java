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

package org.eclipse.edc.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;
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
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class CatalogRuntimeComponentTest {

    public static final String TEST_CATALOG_ID = "test-catalog-id";
    private static final Duration TEST_TIMEOUT = ofSeconds(10);
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    private static final JsonLd JSON_LD_SERVICE = new TitaniumJsonLd(mock());

    @RegisterExtension
    protected static RuntimeExtension runtimePerClassExtension = new RuntimePerMethodExtension(
            new EmbeddedRuntime("catalog", ":dist:bom:federatedcatalog-base-bom")
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                        // make sure only one crawl-run is performed
                        "edc.catalog.cache.execution.period.seconds", "2",
                        // number of crawlers will be limited by the number of crawl-targets
                        "edc.catalog.cache.partition.num.crawlers", "10",
                        // give the runtime time to set up everything
                        "edc.catalog.cache.execution.delay.seconds", "1",
                        "web.http.catalog.port", valueOf(TestFunctions.CATALOG_QUERY_PORT),
                        "web.http.catalog.path", TestFunctions.CATALOG_QUERY_BASE_PATH,
                        "web.http.port", valueOf(getFreePort()),
                        "web.http.path", "/api/v1",
                        "web.http.protocol.port", valueOf(getFreePort()),
                        "web.http.protocol.path", "/api/v1/dsp",
                        "edc.participant.id", "test-participant"
                    )))
    );
    private final DspHttpRemoteMessageDispatcher dispatcher = mock();

    @Test
    @DisplayName("Crawl a single target, yields no results")
    void crawlSingle_noResults(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());
        // intercept request egress
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog)));

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var response = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(response).hasSize(1);
                    assertThat(response).allSatisfy(c -> assertThat(c.getDatasets()).isNullOrEmpty());
                });
    }

    @Test
    @DisplayName("Crawl a single target, yields some results")
    void crawlSingle_withResults(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());
        // intercept request egress
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(randomCatalog(catalog -> toBytes(ttr, catalog), TEST_CATALOG_ID, 5))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog))); // this is important, otherwise there is an endless loop!

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs).allSatisfy(c -> assertThat(c.getDatasets()).hasSize(5));
                });
    }

    @Test
    @DisplayName("Crawl a single target, returns a catalog of catalogs")
    void crawlSingle_withCatalogOfCatalogs(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());
        // intercept request egress
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(randomCatalog(catalog -> StatusResult.success(TestUtils.getResourceFileContentAsString("catalog_of_catalogs.json").getBytes()), "root-catalog-id", 5))
                .thenReturn(randomCatalog(catalog -> StatusResult.success(TestUtils.getResourceFileContentAsString("catalog.json").getBytes()), "sub-catalog-id", 5));

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs).isNotEmpty().allSatisfy(c -> {
                        assertThat(c.getDatasets()).hasSize(2);
                        assertThat(c.getDatasets()).anySatisfy(ds -> assertThat(ds).isInstanceOf(Catalog.class));
                    });
                });
    }

    @Test
    @DisplayName("Crawl a single targets, > 100 results, needs paging")
    void crawlSingle_withPagedResults(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());

        // intercept request egress
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(randomCatalog(catalog -> toBytes(ttr, catalog), TEST_CATALOG_ID, 100))
                .thenReturn(randomCatalog(catalog -> toBytes(ttr, catalog), TEST_CATALOG_ID, 100))
                .thenReturn(randomCatalog(catalog -> toBytes(ttr, catalog), TEST_CATALOG_ID, 50));

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs.size()).isEqualTo(1);
                    assertThat(catalogs.get(0).getDatasets()).hasSize(250);
                });
        verify(dispatcher, atLeast(3)).dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class));

    }

    @Test
    @DisplayName("Crawl a single target twice, emulate deletion of assets")
    void crawlSingle_withDeletions_shouldRemove(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());

        // intercept request egress
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(completedFuture(toBytes(ttr, catalogBuilder().id(TEST_CATALOG_ID).datasets(new ArrayList<>(List.of(
                        createDataset("offer1"), createDataset("offer2"), createDataset("offer3")
                ))).build())))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog), TEST_CATALOG_ID))
                .thenReturn(completedFuture(toBytes(ttr, catalogBuilder().id(TEST_CATALOG_ID).datasets(new ArrayList<>(List.of(
                        createDataset("offer1"), createDataset("offer2")/* this one is "deleted": createDataset("offer3") */
                ))).build())));

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs).hasSize(1);
                    assertThat(catalogs.get(0).getDatasets()).hasSize(2)
                            .noneMatch(offer -> offer.getId().equals("offer3"));
                    verify(dispatcher, atLeast(4)).dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class));
                });

    }

    @Test
    @DisplayName("Crawl a single target twice, emulate deleting and re-adding of assets with same ID")
    void crawlSingle_withUpdates_shouldReplace(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());

        // intercept request egress
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(completedFuture(toBytes(ttr, catalogBuilder().id(TEST_CATALOG_ID).datasets(new ArrayList<>(List.of(
                        createDataset("offer1"), createDataset("offer2"), createDataset("offer3")
                ))).build())))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog), TEST_CATALOG_ID))
                .thenReturn(completedFuture(toBytes(ttr, catalogBuilder().id(TEST_CATALOG_ID).datasets(new ArrayList<>(List.of(
                        createDataset("offer1"), createDataset("offer2"), createDataset("offer3")
                ))).build())));

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs).hasSize(1);
                    assertThat(catalogs.get(0).getDatasets()).hasSize(3);
                    verify(dispatcher, atLeast(4)).dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class));
                });

    }

    @Test
    @DisplayName("Crawl a single target twice, emulate addition of assets")
    void crawlSingle_withAdditions_shouldAdd(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());

        // intercept request egress
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenAnswer(a -> completedFuture(toBytes(ttr, catalogBuilder().id("test-cat")
                        .datasets(List.of(createDataset("dataset1"), createDataset("dataset2"))).build())))
                .thenAnswer(a -> completedFuture(toBytes(ttr, catalogBuilder().id("test-cat")
                        .datasets(List.of(createDataset("dataset1"), createDataset("dataset2"),
                                createDataset("dataset3"), createDataset("dataset4"))).build())));

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
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
    void crawlSingle_verifyCorrectOriginator(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        // prepare node directory
        directory.insert(targetNode());
        // intercept request egress
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(randomCatalog(catalog -> toBytes(ttr, catalog), TEST_CATALOG_ID, 5))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog))); // this is important, otherwise there is an endless loop!

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs).hasSize(1);
                    assertThat(catalogs.get(0).getDatasets()).hasSize(5);
                    assertThat(catalogs).extracting(Catalog::getProperties).allSatisfy(a -> assertThat(a).containsEntry(PROPERTY_ORIGINATOR, "http://test-node.com"));
                });
    }

    @Test
    @DisplayName("Crawl 1000 targets, verify that all offers are collected")
    void crawlMany_shouldCollectAll(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {

        var numTotalAssets = new AtomicInteger();
        var rnd = new SecureRandom();

        // create 1000 crawl targets, setup dispatcher mocks for them
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);
        var numTargets = 50;
        range(0, numTargets)
                .forEach(i -> {
                    var nodeId = "did:web:" + UUID.randomUUID();
                    var nodeUrl = format("http://test-node%s.com", i);
                    var node = new TargetNode("test-node-" + i, nodeId, nodeUrl, singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));
                    directory.insert(node);

                    var numAssets = 1 + rnd.nextInt(10);
                    when(dispatcher.dispatch(any(), eq(byte[].class), argThat(sentTo(nodeId, nodeUrl))))
                            .thenReturn(randomCatalog(catalog -> toBytes(ttr, catalog), "catalog-" + nodeUrl, numAssets));
                    numTotalAssets.addAndGet(numAssets);
                });

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT.plus(TEST_TIMEOUT))
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs).hasSize(numTargets);
                    //assert that the total number of offers across all catalogs is corrects
                    assertThat(catalogs.stream().mapToLong(c -> c.getDatasets().size()).sum()).isEqualTo(numTotalAssets.get());
                });
    }

    @Test
    @DisplayName("Crawl multiple targets with conflicting asset IDs")
    void crawlMultiple_whenConflictingAssetIds_shouldOverwrite(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        var node1 = new TargetNode("test-node1", "did:web:" + UUID.randomUUID(), "http://test-node1.com", singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));
        var node2 = new TargetNode("test-node2", "did:web:" + UUID.randomUUID(), "http://test-node2.com", singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));

        directory.insert(node1);
        directory.insert(node2);
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);

        when(dispatcher.dispatch(any(), eq(byte[].class), argThat(sentTo(node1.id(), node1.targetUrl()))))
                .thenReturn(catalogOf(catalog -> toBytes(ttr, catalog), "catalog-" + node1.targetUrl(), createDataset("offer1"), createDataset("offer2"), createDataset("offer3")))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog)));

        when(dispatcher.dispatch(any(), eq(byte[].class), argThat(sentTo(node2.id(), node2.targetUrl()))))
                .thenReturn(catalogOf(catalog -> toBytes(ttr, catalog), "catalog-" + node2.targetUrl(), createDataset("offer14"), createDataset("offer32"), /*this one is conflicting:*/createDataset("offer3")))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog)));

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs).hasSize(2);
                    assertThat(catalogs).anySatisfy(c -> assertThat(c.getProperties().get(PROPERTY_ORIGINATOR).toString()).startsWith("http://test-node1.com"));
                    assertThat(catalogs).anySatisfy(c -> assertThat(c.getProperties().get(PROPERTY_ORIGINATOR).toString()).startsWith("http://test-node2.com"));
                    assertThat(catalogs.stream().mapToLong(c -> c.getDatasets().size()).sum()).isEqualTo(6);
                    assertThat(catalogs.stream().flatMap(c -> c.getDatasets().stream()).map(Dataset::getId))
                            .containsExactlyInAnyOrder("offer1", "offer2", "offer3", "offer14", "offer32", "offer3");
                });


    }

    private StatusResult<byte[]> toBytes(TypeTransformerRegistry transformerRegistry, Catalog cat1) {
        try {
            var jo = transformerRegistry.transform(cat1, JsonObject.class).orElseThrow(AssertionError::new);
            var expanded = JSON_LD_SERVICE.expand(jo).orElseThrow(AssertionError::new);
            var expandedStr = OBJECT_MAPPER.writeValueAsString(expanded);
            return StatusResult.success(expandedStr.getBytes());
        } catch (JsonProcessingException ex) {
            throw new AssertionError(ex);
        }
    }

    private @NotNull TargetNode targetNode() {
        return new TargetNode("test-node", "did:web:" + UUID.randomUUID(), "http://test-node.com", singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));
    }

}
