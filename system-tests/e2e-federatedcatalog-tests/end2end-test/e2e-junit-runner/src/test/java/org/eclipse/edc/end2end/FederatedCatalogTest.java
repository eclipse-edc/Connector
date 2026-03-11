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

package org.eclipse.edc.end2end;

import jakarta.json.Json;
import org.eclipse.edc.catalog.transform.JsonObjectToCatalogTransformer;
import org.eclipse.edc.catalog.transform.JsonObjectToDataServiceTransformer;
import org.eclipse.edc.catalog.transform.JsonObjectToDatasetTransformer;
import org.eclipse.edc.catalog.transform.JsonObjectToDistributionTransformer;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.transform.odrl.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.connector.core.agent.NoOpParticipantIdMapper;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.v2025.from.JsonObjectFromCatalogV2025Transformer;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.Duration.ofSeconds;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transform.odrl.OdrlTransformersFactory.jsonObjectToOdrlTransformers;
import static org.eclipse.edc.end2end.TestFunctions.createContractDef;
import static org.eclipse.edc.end2end.TestFunctions.createPolicy;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_CONTEXT_2025_1;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_VERSION;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@EndToEndTest
class FederatedCatalogTest {

    public static final Duration TIMEOUT = ofSeconds(30);
    private static final Endpoint CONNECTOR_MANAGEMENT = new Endpoint("/management", "8081");
    private static final Endpoint CONNECTOR_PROTOCOL = new Endpoint("/api/v1/dsp", "8082");
    private static final Endpoint CONNECTOR_DEFAULT = new Endpoint("/api/v1/", "8080");
    private static final Endpoint CONNECTOR_CONTROL = new Endpoint("/api/v1/control", "8083");

    private static final Endpoint CATALOG_MANAGEMENT = new Endpoint("/management", "8091");
    private static final Endpoint CATALOG_PROTOCOL = new Endpoint("/api/v1/dsp", "8092");
    private static final Endpoint CATALOG_DEFAULT = new Endpoint("/api/v1/", "8090");
    private static final Endpoint CATALOG_CATALOG = new Endpoint("/catalog", "8093");

    @RegisterExtension
    static RuntimeExtension connector = new RuntimePerClassExtension(
            new EmbeddedRuntime("connector", ":system-tests:e2e-federatedcatalog-tests:end2end-test:connector-runtime")
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                            entry("edc.connector.name", "connector1"),
                            entry("edc.web.rest.cors.enabled", "true"),
                            entry("web.http.port", CONNECTOR_DEFAULT.port()),
                            entry("web.http.path", CONNECTOR_DEFAULT.path()),
                            entry("web.http.protocol.port", CONNECTOR_PROTOCOL.port()),
                            entry("web.http.protocol.path", CONNECTOR_PROTOCOL.path()),
                            entry("web.http.control.port", CONNECTOR_CONTROL.port()),
                            entry("web.http.control.path", CONNECTOR_CONTROL.path()),
                            entry("web.http.management.port", CONNECTOR_MANAGEMENT.port()),
                            entry("edc.participant.id", "test-connector"),
                            entry("web.http.management.path", CONNECTOR_MANAGEMENT.path()),
                            entry("edc.web.rest.cors.headers", "origin,content-type,accept,authorization,x-api-key"),
                            entry("edc.dsp.callback.address", "http://localhost:%s%s".formatted(CONNECTOR_PROTOCOL.port(), CONNECTOR_PROTOCOL.path()))
                    )))
    );

    @RegisterExtension
    static RuntimeExtension catalog = new RuntimePerMethodExtension(
            new EmbeddedRuntime("catalog", ":dist:bom:federatedcatalog-base-bom", ":extensions:common:iam:iam-mock")
                    .configurationProvider(() -> ConfigFactory.fromMap(ofEntries(
                            entry("edc.catalog.cache.execution.delay.seconds", "0"),
                            entry("edc.catalog.cache.execution.period.seconds", "5"),
                            entry("edc.catalog.cache.partition.num.crawlers", "3"),
                            entry("edc.web.rest.cors.enabled", "true"),
                            entry("edc.participant.id", "test-catalog"),
                            entry("web.http.port", CATALOG_DEFAULT.port()),
                            entry("web.http.path", CATALOG_DEFAULT.path()),
                            entry("web.http.protocol.port", CATALOG_PROTOCOL.port()),
                            entry("web.http.protocol.path", CATALOG_PROTOCOL.path()),
                            entry("web.http.management.port", CATALOG_MANAGEMENT.port()),
                            entry("web.http.management.path", CATALOG_MANAGEMENT.path()),
                            entry("web.http.version.port", getFreePort() + ""),
                            entry("web.http.version.path", "/.well-known/version"),
                            entry("web.http.catalog.port", CATALOG_CATALOG.port()),
                            entry("web.http.catalog.path", CATALOG_CATALOG.path()),
                            entry("edc.web.rest.cors.headers", "origin,content-type,accept,authorization,x-api-key")
                    )))
    );

    private final TypeTransformerRegistry typeTransformerRegistry = new TypeTransformerRegistryImpl();
    private final TypeManager mapper = new JacksonTypeManager();
    private final CatalogApiClient apiClient = new CatalogApiClient(CATALOG_CATALOG, CONNECTOR_MANAGEMENT,
            JacksonJsonLd.createObjectMapper(), () -> catalog.getService(JsonLd.class), typeTransformerRegistry);

    @BeforeEach
    void setUp() {
        var factory = Json.createBuilderFactory(Map.of());
        var participantIdMapper = new NoOpParticipantIdMapper();
        typeTransformerRegistry.register(new JsonObjectFromCatalogV2025Transformer(factory, new JacksonTypeManager(), JSON_LD, participantIdMapper, DSP_NAMESPACE_V_2025_1));
        typeTransformerRegistry.register(new JsonObjectFromDatasetTransformer(factory, mapper, JSON_LD));
        typeTransformerRegistry.register(new JsonObjectFromDataServiceTransformer(factory));
        typeTransformerRegistry.register(new JsonObjectFromPolicyTransformer(factory, participantIdMapper));
        typeTransformerRegistry.register(new JsonObjectFromDistributionTransformer(factory));
        typeTransformerRegistry.register(new JsonObjectToCatalogTransformer());
        typeTransformerRegistry.register(new JsonObjectToDatasetTransformer());
        typeTransformerRegistry.register(new JsonObjectToDataServiceTransformer());
        jsonObjectToOdrlTransformers(participantIdMapper).forEach(typeTransformerRegistry::register);
        typeTransformerRegistry.register(new JsonObjectToDistributionTransformer());
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(mapper, JSON_LD));

        var node = new TargetNode(
                "connector", "did:web:" + UUID.randomUUID(),
                "http://localhost:%s%s".formatted(CONNECTOR_PROTOCOL.port(), CONNECTOR_PROTOCOL.path() + "/" + V_2025_1_VERSION),
                List.of(DATASPACE_PROTOCOL_HTTP_V_2025_1)
        );
        catalog.registerSystemExtension(ServiceExtension.class, new SeedNodeExtension(node));
    }

    @Test
    void crawl_whenOfferAvailable_shouldContainOffer(TestInfo testInfo) {
        var id = testInfo.getDisplayName() + "-" + UUID.randomUUID();
        var asset = TestFunctions.createAssetJson(id);
        var r = apiClient.postAsset(asset);
        assertThat(r).withFailMessage(getError(r)).isSucceeded();

        var assetId = r.getContent();

        var policy = createPolicy("policy-" + id, id);
        var pr = apiClient.postPolicy(policy);
        assertThat(r).withFailMessage(getError(pr)).isSucceeded();

        var policyId = pr.getContent();

        var request = createContractDef("def-" + id, policyId, policyId, assetId);

        var dr = apiClient.postContractDefinition(request);
        assertThat(dr).withFailMessage(getError(dr)).isSucceeded();

        var assetIdBase64 = Base64.getEncoder().encodeToString(assetId.getBytes());

        await().pollDelay(ofSeconds(1))
                .pollInterval(ofSeconds(1))
                .atMost(TIMEOUT)
                .untilAsserted(() -> {
                    var emptyQuery = TestFunctions.createEmptyQuery();
                    var catalogsJson = apiClient.queryCatalogs(emptyQuery);
                    assertThat(catalogsJson).contains(DSPACE_CONTEXT_2025_1);

                    var catalogs = apiClient.deserializeCatalogs(catalogsJson);
                    assertCatalogContainsOffer(assetIdBase64, catalogs);
                });

        await().pollDelay(ofSeconds(1))
                .pollInterval(ofSeconds(1))
                .atMost(TIMEOUT)
                .untilAsserted(() -> {
                    var queryWithExistingAssetId = TestFunctions.createQuerySpecWithFilterExpressionForAssetId(id);
                    var catalogs = apiClient.deserializeCatalogs(apiClient.queryCatalogs(queryWithExistingAssetId));

                    assertCatalogContainsOffer(assetIdBase64, catalogs);
                });
    }

    private String getError(Result<String> r) {
        return ofNullable(r.getFailureDetail()).orElse("No error");
    }

    private void assertCatalogContainsOffer(String assetIdBase64, List<Catalog> catalogs) {
        assertThat(catalogs).hasSizeGreaterThanOrEqualTo(1);
        assertThat(catalogs).anySatisfy(catalog -> assertThat(catalog.getDatasets())
                .anySatisfy(dataset -> {
                    assertThat(dataset.getOffers()).hasSizeGreaterThanOrEqualTo(1);
                    assertThat(dataset.getOffers().keySet()).anyMatch(key -> key.contains(assetIdBase64));
                }));
    }

    private static class SeedNodeExtension implements ServiceExtension {

        private final TargetNode node;

        @Inject
        private TargetNodeDirectory targetNodeDirectory;

        SeedNodeExtension(TargetNode node) {
            this.node = node;
        }

        @Override
        public void prepare() {
            targetNodeDirectory.insert(node);
        }

    }
}
