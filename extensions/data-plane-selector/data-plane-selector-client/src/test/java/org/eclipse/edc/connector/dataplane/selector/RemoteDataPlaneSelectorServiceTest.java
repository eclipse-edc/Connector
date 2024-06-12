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

package org.eclipse.edc.connector.dataplane.selector;

import jakarta.json.Json;
import org.eclipse.edc.api.auth.spi.ControlClientAuthenticationProvider;
import org.eclipse.edc.api.transformer.JsonObjectFromIdResponseTransformer;
import org.eclipse.edc.connector.dataplane.selector.control.api.DataplaneSelectorControlApiController;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.transformer.JsonObjectToSelectionRequestTransformer;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromDataPlaneInstanceTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToDataPlaneInstanceTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class RemoteDataPlaneSelectorServiceTest extends RestControllerTestBase {

    private static final String[] FIELDS_TO_BE_IGNORED = {"createdAt", "stateTimestamp", "updatedAt"};
    private final String url = "http://localhost:%d/v1/dataplanes".formatted(port);
    private final ControlClientAuthenticationProvider authenticationProvider = mock();
    private final DataPlaneSelectorService serverService = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = new TypeTransformerRegistryImpl();
    private final JsonObjectValidatorRegistry validator = mock();
    private final RemoteDataPlaneSelectorService service = new RemoteDataPlaneSelectorService(testHttpClient(), url,
            JacksonJsonLd.createObjectMapper(), typeTransformerRegistry, "selectionStrategy", authenticationProvider);

    @BeforeEach
    void setUp() {
        var factory = Json.createBuilderFactory(Map.of());
        var objectMapper = JacksonJsonLd.createObjectMapper();
        typeTransformerRegistry.register(new JsonObjectFromDataAddressTransformer(factory));
        typeTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
        typeTransformerRegistry.register(new JsonObjectToSelectionRequestTransformer());
        typeTransformerRegistry.register(new JsonObjectFromDataPlaneInstanceTransformer(factory, JacksonJsonLd.createObjectMapper()));
        typeTransformerRegistry.register(new JsonObjectToDataPlaneInstanceTransformer());
        typeTransformerRegistry.register(new JsonObjectFromIdResponseTransformer(factory));
        typeTransformerRegistry.register(new org.eclipse.edc.connector.dataplane.selector.control.api.transformer.JsonObjectToSelectionRequestTransformer());
        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(objectMapper));
    }

    @Test
    void addInstance() {
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(serverService.addInstance(any())).thenReturn(ServiceResult.success());
        var instance = createInstance("dataPlaneId");

        var result = service.addInstance(instance);

        assertThat(result).isSucceeded();
        verify(serverService).addInstance(any());
        verify(authenticationProvider).authenticationHeaders();
    }

    @Test
    void select() {
        var expected = createInstance("some-instance");
        when(serverService.select(any(), eq("transferType"), eq("random"))).thenReturn(ServiceResult.success(expected));

        var result = service.select(DataAddress.Builder.newInstance().type("test1").build(), "transferType", "random");

        assertThat(result).isSucceeded().usingRecursiveComparison()
                .ignoringFields(FIELDS_TO_BE_IGNORED).isEqualTo(expected);
        verify(authenticationProvider).authenticationHeaders();
    }

    @Nested
    class Unregister {
        @Test
        void shouldUnregister() {
            var instanceId = UUID.randomUUID().toString();
            when(serverService.unregister(any())).thenReturn(ServiceResult.success());

            var result = service.unregister(instanceId);

            assertThat(result).isSucceeded();
            verify(serverService).unregister(instanceId);
        }

        @Test
        void shouldFail_whenServiceFails() {
            var instanceId = UUID.randomUUID().toString();
            when(serverService.unregister(any())).thenReturn(ServiceResult.conflict("conflict"));

            var result = service.unregister(instanceId);

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDelete() {
            var instanceId = UUID.randomUUID().toString();
            when(serverService.delete(any())).thenReturn(ServiceResult.success());

            var result = service.delete(instanceId);

            assertThat(result).isSucceeded();
            verify(serverService).delete(instanceId);
        }

        @Test
        void shouldFail_whenNotFound() {
            var instanceId = UUID.randomUUID().toString();
            when(serverService.delete(any())).thenReturn(ServiceResult.notFound("not found"));

            var result = service.delete(instanceId);

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        }
    }

    @Nested
    class FindById {
        @Test
        void shouldReturnInstanceById() {
            var instanceId = UUID.randomUUID().toString();
            var instance = DataPlaneInstance.Builder.newInstance().url("http://any").build();
            when(serverService.findById(any())).thenReturn(ServiceResult.success(instance));

            var result = service.findById(instanceId);

            assertThat(result).isSucceeded().usingRecursiveComparison()
                    .ignoringFields(FIELDS_TO_BE_IGNORED)
                    .isEqualTo(instance);
            verify(authenticationProvider).authenticationHeaders();
        }

        @Test
        void shouldReturnNotFound_whenInstanceDoesNotExist() {
            var instanceId = UUID.randomUUID().toString();
            when(serverService.findById(any())).thenReturn(ServiceResult.notFound("not found"));

            var result = service.findById(instanceId);

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        }
    }

    @Override
    protected Object controller() {
        return new DataplaneSelectorControlApiController(validator, typeTransformerRegistry, serverService, Clock.systemUTC());
    }

    private DataPlaneInstance createInstance(String id) {
        return DataPlaneInstance.Builder.newInstance()
                .id(id)
                .url("http://somewhere.com:1234/api/v1")
                .build();
    }
}
