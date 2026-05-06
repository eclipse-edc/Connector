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
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.valueOf;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
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
import static org.mockito.Mockito.mock;
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
    private static final JsonLd JSON_LD_SERVICE = new TitaniumJsonLd(mock());

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
                            Map.entry("web.http.control.port", valueOf(getFreePort())),
                            Map.entry("web.http.control.path", "/api/control"),
                            Map.entry("edc.participant.id", "test-participant")
                    )))
    );

    private final DspHttpRemoteMessageDispatcher dispatcher = mock();

    @Test
    @DisplayName("Verify crawler inside control plane: crawl single target, no results")
    void crawlSingle_noResults(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        directory.insert(targetNode());
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
    @DisplayName("Verify crawler inside control plane: crawl single target, yields results")
    void crawlSingle_withResults(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        directory.insert(targetNode());
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(randomCatalog(catalog -> toBytes(ttr, catalog), "test-catalog-id", 5))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog)));

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs).allSatisfy(c -> assertThat(c.getDatasets()).hasSize(5));
                });
    }

    @Test
    @DisplayName("Verify crawler inside control plane: crawl single target, correct originator")
    void crawlSingle_verifyOriginator(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        directory.insert(targetNode());
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(randomCatalog(catalog -> toBytes(ttr, catalog), "test-catalog-id", 3))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog)));

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs).hasSize(1);
                    assertThat(catalogs.get(0).getDatasets()).hasSize(3);
                    assertThat(catalogs).extracting(Catalog::getProperties)
                            .allSatisfy(a -> assertThat(a).containsEntry(PROPERTY_ORIGINATOR, "http://test-node.com"));
                });
    }

    @Test
    @DisplayName("Verify crawler inside control plane: crawl multiple targets with distinct catalogs")
    void crawlMultiple_shouldCollectAll(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        var node1 = new TargetNode("test-node1", "did:web:" + UUID.randomUUID(), "http://test-node1.com", singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));
        var node2 = new TargetNode("test-node2", "did:web:" + UUID.randomUUID(), "http://test-node2.com", singletonList(DATASPACE_PROTOCOL_HTTP_V_2025_1));
        directory.insert(node1);
        directory.insert(node2);
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);

        when(dispatcher.dispatch(any(), eq(byte[].class), argThat(sentTo(node1.id(), node1.targetUrl()))))
                .thenReturn(catalogOf(catalog -> toBytes(ttr, catalog), "catalog-node1", createDataset("offer1"), createDataset("offer2")))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog)));
        when(dispatcher.dispatch(any(), eq(byte[].class), argThat(sentTo(node2.id(), node2.targetUrl()))))
                .thenReturn(catalogOf(catalog -> toBytes(ttr, catalog), "catalog-node2", createDataset("offer3")))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog)));

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs).hasSize(2);
                    assertThat(catalogs.stream().flatMap(c -> c.getDatasets().stream()).map(Dataset::getId))
                            .containsExactlyInAnyOrder("offer1", "offer2", "offer3");
                });
    }

    @Test
    @DisplayName("Verify crawler inside control plane: crawl with asset deletions")
    void crawlSingle_withDeletions(RemoteMessageDispatcherRegistry reg, TypeTransformerRegistry ttr, TargetNodeDirectory directory, JsonLd jsonLd) {
        directory.insert(targetNode());
        reg.register(DATASPACE_PROTOCOL_HTTP_V_2025_1, dispatcher);

        var catalogId = "test-catalog-id";
        when(dispatcher.dispatch(any(), eq(byte[].class), isA(CatalogRequestMessage.class)))
                .thenReturn(completedFuture(toBytes(ttr, catalogBuilder().id(catalogId).datasets(new ArrayList<>(List.of(
                        createDataset("offer1"), createDataset("offer2"), createDataset("offer3")
                ))).build())))
                .thenReturn(emptyCatalog(catalog -> toBytes(ttr, catalog), catalogId))
                .thenReturn(completedFuture(toBytes(ttr, catalogBuilder().id(catalogId).datasets(new ArrayList<>(List.of(
                        createDataset("offer1"), createDataset("offer2")
                ))).build())));

        await().pollDelay(ofSeconds(1))
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    var catalogs = queryCatalogApi(jsonLd, jsonObject -> ttr.transform(jsonObject, Catalog.class).orElseThrow(AssertionError::new));
                    assertThat(catalogs).hasSize(1);
                    assertThat(catalogs.get(0).getDatasets()).hasSize(2)
                            .noneMatch(offer -> offer.getId().equals("offer3"));
                });
    }

    private StatusResult<byte[]> toBytes(TypeTransformerRegistry transformerRegistry, Catalog catalog) {
        try {
            var jo = transformerRegistry.transform(catalog, JsonObject.class).orElseThrow(AssertionError::new);
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
