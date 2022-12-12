/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.dataplane.selector.DataPlaneSelectorServiceImpl;
import org.eclipse.edc.connector.dataplane.selector.core.DataPlaneSelectorImpl;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceImpl;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategy;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.connector.dataplane.selector.store.InMemoryDataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.strategy.DefaultSelectionStrategyRegistry;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.jersey.JerseyConfiguration;
import org.eclipse.edc.web.jersey.JerseyRestService;
import org.eclipse.edc.web.jetty.JettyConfiguration;
import org.eclipse.edc.web.jetty.JettyService;
import org.eclipse.edc.web.jetty.PortMapping;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.TestFunctions.createInstance;
import static org.eclipse.edc.connector.dataplane.selector.TestFunctions.createInstanceBuilder;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.mockito.Mockito.mock;

@ApiTest
class DataplaneSelectorApiControllerIntegrationTest {

    public static final MediaType JSON_TYPE = MediaType.parse("application/json");
    private static int port;
    private static InMemoryDataPlaneInstanceStore store;
    private static JettyConfiguration config;
    private static Monitor monitor;
    private final TypeReference<List<DataPlaneInstance>> listTypeRef = new TypeReference<>() {
    };
    private ObjectMapper objectMapper;
    private EdcHttpClient client;
    private JettyService jetty;
    private SelectionStrategyRegistry selectionStrategyRegistry;

    @BeforeAll
    static void prepareWebserver() {
        port = TestUtils.getFreePort();
        monitor = mock(Monitor.class);
        config = new JettyConfiguration(null, null);
        config.portMapping(new PortMapping("dataplane", port, "/api/v1/dataplane"));
    }

    @BeforeEach
    void setup() {
        client = testHttpClient();

        selectionStrategyRegistry = new DefaultSelectionStrategyRegistry(); //in-memory

        store = new InMemoryDataPlaneInstanceStore();
        var selector = new DataPlaneSelectorImpl(store);
        var service = new DataPlaneSelectorServiceImpl(selector, store, selectionStrategyRegistry);
        var controller = new DataplaneSelectorApiController(service);

        jetty = new JettyService(config, monitor);

        TypeManager typeManager = new TypeManager();
        typeManager.registerTypes(DataPlaneInstanceImpl.class);
        objectMapper = typeManager.getMapper();
        JerseyRestService jerseyService = new JerseyRestService(jetty, typeManager, mock(JerseyConfiguration.class), monitor);
        jetty.start();
        jerseyService.registerResource("dataplane", controller);
        jerseyService.start();
    }

    @AfterEach
    void teardown() {
        jetty.shutdown();
    }

    @Test
    void getAll() throws IOException {
        var list = List.of(createInstance("test-id1"), createInstance("test-id2"), createInstance("test-id3"));
        store.saveAll(list);

        var response = get(basePath());


        assertThat(response.code()).isEqualTo(200);
        var result = objectMapper.readValue(response.body().string(), listTypeRef);
        assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(list.toArray(new DataPlaneInstance[0]));
    }

    @Test
    void getAll_noneExist() throws IOException {
        try (var response = get(basePath())) {

            assertThat(response.code()).isEqualTo(200);
            var result = objectMapper.readValue(response.body().string(), listTypeRef);
            assertThat(result).isNotNull().isEmpty();
        }
    }

    @Test
    void addEntry() throws IOException {
        var dpi = createInstance("test-id");

        var body = RequestBody.create(objectMapper.writeValueAsString(dpi), JSON_TYPE);
        try (var response = post(basePath(), body)) {
            assertThat(response.isSuccessful()).isTrue();
            assertThat(store.getAll()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(dpi);
        }
    }

    @Test
    void addEntry_exists_shouldOverwrite() throws IOException {
        var dpi1 = createInstance("test-id1");
        var dpi2 = createInstance("test-id2");
        var dpi3 = createInstance("test-id3");
        store.saveAll(List.of(dpi1, dpi2, dpi3));


        var newDpi = createInstanceBuilder("test-id2")
                .allowedDestType("test-dest-type")
                .build();


        var body = RequestBody.create(objectMapper.writeValueAsString(newDpi), JSON_TYPE);
        try (var response = post(basePath(), body)) {
            assertThat(response.isSuccessful()).isTrue();
            assertThat(store.getAll()).hasSize(3).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(dpi1, dpi3, newDpi).doesNotContain(dpi2);
        }
    }

    @Test
    void select() throws IOException {
        var dpi = createInstanceBuilder("test-id")
                .allowedSourceType("test-src1")
                .allowedDestType("test-dst1")
                .build();
        var dpi2 = createInstanceBuilder("test-id")
                .allowedSourceType("test-src2")
                .allowedDestType("test-dst2")
                .build();
        store.saveAll(List.of(dpi, dpi2));

        var src = DataAddress.Builder.newInstance().type("test-src1").build();
        var dest = DataAddress.Builder.newInstance().type("test-dst1").build();
        var rq = new SelectionRequest(src, dest);


        var body = RequestBody.create(objectMapper.writeValueAsString(rq), JSON_TYPE);
        try (var response = post(basePath() + "/select", body)) {
            assertThat(response.isSuccessful()).isTrue();
            var result = objectMapper.readValue(response.body().string(), DataPlaneInstance.class);
            assertThat(result).usingRecursiveComparison().isEqualTo(dpi);
        }
    }

    @Test
    void select_noneCanHandle_shouldReturnEmpty() throws IOException {
        var dpi = createInstanceBuilder("test-id")
                .allowedSourceType("test-src1")
                .allowedDestType("test-dst1")
                .build();
        var dpi2 = createInstanceBuilder("test-id")
                .allowedSourceType("test-src2")
                .allowedDestType("test-dst2")
                .build();
        store.saveAll(List.of(dpi, dpi2));

        var src = DataAddress.Builder.newInstance().type("notexist-src").build();
        var dest = DataAddress.Builder.newInstance().type("test-dst1").build();
        var rq = new SelectionRequest(src, dest);


        var body = RequestBody.create(objectMapper.writeValueAsString(rq), JSON_TYPE);
        try (var response = post(basePath() + "/select", body)) {
            assertThat(response.isSuccessful()).isTrue();
            assertThat(response.body().string()).isEmpty();
        }
    }

    @Test
    void select_selectionStrategyNotFound() throws IOException {
        var dpi = createInstanceBuilder("test-id")
                .allowedSourceType("test-src1")
                .allowedDestType("test-dst1")
                .build();
        var dpi2 = createInstanceBuilder("test-id")
                .allowedSourceType("test-src2")
                .allowedDestType("test-dst2")
                .build();
        store.saveAll(List.of(dpi, dpi2));

        var src = DataAddress.Builder.newInstance().type("test-src1").build();
        var dest = DataAddress.Builder.newInstance().type("test-dst1").build();
        var rq = new SelectionRequest(src, dest, "non-exist");


        var body = RequestBody.create(objectMapper.writeValueAsString(rq), JSON_TYPE);
        try (var response = post(basePath() + "/select", body)) {
            // TODO: this should return 400 but currently the request is not validated correctly by the `DataplaneSelectorApiController`
            assertThat(response.code()).isEqualTo(500);
        }
    }

    @Test
    void select_withCustomStrategy() throws IOException {
        var dpi = createInstanceBuilder("test-id")
                .allowedSourceType("test-src1")
                .allowedDestType("test-dst1")
                .build();
        var dpi2 = createInstanceBuilder("test-id")
                .allowedSourceType("test-src1")
                .allowedDestType("test-dst1")
                .build();
        store.saveAll(List.of(dpi, dpi2));

        var myCustomStrategy = new SelectionStrategy() {
            @Override
            public DataPlaneInstance apply(List<DataPlaneInstance> instances) {
                return instances.get(0);
            }

            @Override
            public String getName() {
                return "myCustomStrategy";
            }
        };

        selectionStrategyRegistry.add(myCustomStrategy);

        var src = DataAddress.Builder.newInstance().type("test-src1").build();
        var dest = DataAddress.Builder.newInstance().type("test-dst1").build();
        var rq = new SelectionRequest(src, dest, "myCustomStrategy");


        var body = RequestBody.create(objectMapper.writeValueAsString(rq), JSON_TYPE);
        try (var response = post(basePath() + "/select", body)) {
            assertThat(response.isSuccessful()).isTrue();
            assertThat(objectMapper.readValue(response.body().string(), DataPlaneInstance.class)).usingRecursiveComparison().isEqualTo(dpi);
        }
    }

    @NotNull
    private String basePath() {
        return "http://localhost:" + port + "/api/v1/dataplane/instances";
    }

    @NotNull
    private Response get(String url) throws IOException {
        return client.execute(new Request.Builder().get().url(url).build());
    }

    @NotNull
    private Response post(String url, RequestBody requestBody) throws IOException {
        return client.execute(new Request.Builder().post(requestBody).url(url).build());
    }

    @NotNull
    private Response delete(String url) throws IOException {
        return client.execute(new Request.Builder().delete().url(url).build());
    }
}
