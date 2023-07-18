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

package org.eclipse.edc.connector.dataplane.selector.client;

import jakarta.json.Json;
import org.eclipse.edc.connector.dataplane.selector.api.DataplaneSelectorApiController;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectToDataPlaneInstanceTransformer;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectToSelectionRequestTransformer;
import org.eclipse.edc.core.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.selector.TestFunctions.createInstance;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.eclipse.edc.spi.CoreConstants.JSON_LD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class RemoteDataPlaneSelectorClientTest extends RestControllerTestBase {

    private static final String BASE_URL = "http://localhost:%d/v2/dataplanes";
    private static final DataPlaneSelectorService SELECTOR_SERVICE_MOCK = mock();
    private static final TypeManager TYPE_MANAGER = new TypeManager();
    private final TypeTransformerRegistry typeTransformerRegistry = new TypeTransformerRegistryImpl();
    private RemoteDataPlaneSelectorClient client;

    @BeforeAll
    public static void prepare() {
        TYPE_MANAGER.registerTypes(DataPlaneInstance.class);
        TYPE_MANAGER.registerContext(JSON_LD, JacksonJsonLd.createObjectMapper());
    }

    @BeforeEach
    void setUp() {
        var factory = Json.createBuilderFactory(Map.of());
        typeTransformerRegistry.register(new JsonObjectFromDataAddressTransformer(factory));
        typeTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
        typeTransformerRegistry.register(new JsonObjectToSelectionRequestTransformer());
        typeTransformerRegistry.register(new JsonObjectFromDataPlaneInstanceTransformer(factory, JacksonJsonLd.createObjectMapper()));
        typeTransformerRegistry.register(new JsonObjectToDataPlaneInstanceTransformer());
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(objectMapper));
        var url = format(BASE_URL, port);
        client = new RemoteDataPlaneSelectorClient(testHttpClient(), url, JacksonJsonLd.createObjectMapper(), typeTransformerRegistry);
    }

    @Test
    void getAll() {

        when(SELECTOR_SERVICE_MOCK.getAll()).thenReturn(List.of(createInstance("test-inst1"), createInstance("test-inst2")));
        var result = client.getAll();
        assertThat(result).hasSize(2).extracting(DataPlaneInstance::getId).containsExactlyInAnyOrder("test-inst1", "test-inst2");
    }

    @Test
    void find() {
        var expected = createInstance("some-instance");
        when(SELECTOR_SERVICE_MOCK.select(any(), any())).thenReturn(expected);
        var result = client.find(DataAddress.Builder.newInstance().type("test1").build(), DataAddress.Builder.newInstance().type("test2").build());
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);

    }

    @Test
    void find_withStrategy() {
        var expected = createInstance("some-instance");
        when(SELECTOR_SERVICE_MOCK.select(any(), any(), eq("test-strategy"))).thenReturn(expected);
        var result = client.find(DataAddress.Builder.newInstance().type("test1").build(), DataAddress.Builder.newInstance().type("test1").build(), "test-strategy");
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Override
    protected Object controller() {
        return new DataplaneSelectorApiController(SELECTOR_SERVICE_MOCK, typeTransformerRegistry);
    }

    @Override
    protected Object additionalResource() {
        return new JerseyJsonLdInterceptor(new TitaniumJsonLd(mock()), JacksonJsonLd.createObjectMapper());
    }
}
