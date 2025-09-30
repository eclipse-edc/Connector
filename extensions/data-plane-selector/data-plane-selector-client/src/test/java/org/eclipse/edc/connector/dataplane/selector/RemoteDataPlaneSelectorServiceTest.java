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

import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class RemoteDataPlaneSelectorServiceTest {

    private static final String[] FIELDS_TO_BE_IGNORED = {"createdAt", "stateTimestamp", "updatedAt"};
    private final int port = getFreePort();
    @RegisterExtension
    public final RuntimeExtension client = new RuntimePerMethodExtension(new EmbeddedRuntime("client",
            ":core:common:connector-core",
            ":core:common:runtime-core",
            ":extensions:common:http")
            .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "web.http.port", String.valueOf(getFreePort()),
                    "edc.dpf.selector.url", "http://localhost:%d/control/v1/dataplanes".formatted(port),
                    "edc.core.retry.retries.max", "0"
            )))
    );
    private final DataPlaneSelectorService serverService = mock();
    private final JsonObjectValidatorRegistry validator = mock();
    private final SingleParticipantContextSupplier participantContextSupplier = () -> ServiceResult.success(new ParticipantContext("participantContextId"));

    @RegisterExtension
    public final RuntimeExtension server = new RuntimePerMethodExtension(new EmbeddedRuntime(
            "server",
            ":extensions:data-plane-selector:data-plane-selector-control-api",
            ":extensions:common:api:control-api-configuration",
            ":extensions:common:api:api-core",
            ":core:common:connector-core",
            ":core:common:runtime-core",
            ":extensions:common:http")
            .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "web.http.port", String.valueOf(getFreePort()),
                    "edc.dpf.selector.url", "http://not-used-but-mandatory",
                    "web.http.control.port", port + "",
                    "web.http.control.path", "/control"
            )))
            .registerServiceMock(DataPlaneSelectorService.class, serverService)
            .registerServiceMock(JsonObjectValidatorRegistry.class, validator)
            .registerServiceMock(SingleParticipantContextSupplier.class, participantContextSupplier)
    );

    @Test
    void addInstance() {
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());
        when(serverService.addInstance(any())).thenReturn(ServiceResult.success());
        var instance = createInstance("dataPlaneId");

        var result = service().addInstance(instance);

        assertThat(result).isSucceeded();
        verify(serverService).addInstance(any());
    }

    private DataPlaneSelectorService service() {
        return client.getService(DataPlaneSelectorService.class);
    }

    private DataPlaneInstance createInstance(String id) {
        return DataPlaneInstance.Builder.newInstance()
                .id(id)
                .url("http://somewhere.com:1234/api/v1")
                .build();
    }

    @Nested
    class Unregister {

        @Test
        void shouldUnregister() {
            var instanceId = UUID.randomUUID().toString();
            when(serverService.unregister(any())).thenReturn(ServiceResult.success());

            var result = service().unregister(instanceId);

            assertThat(result).isSucceeded();
            verify(serverService).unregister(instanceId);
        }

        @Test
        void shouldFail_whenServiceFails() {
            var instanceId = UUID.randomUUID().toString();
            when(serverService.unregister(any())).thenReturn(ServiceResult.conflict("conflict"));

            var result = service().unregister(instanceId);

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        }

    }

    @Nested
    class Delete {

        @Test
        void shouldDelete() {
            var instanceId = UUID.randomUUID().toString();
            when(serverService.delete(any())).thenReturn(ServiceResult.success());

            var result = service().delete(instanceId);

            assertThat(result).isSucceeded();
            verify(serverService).delete(instanceId);
        }

        @Test
        void shouldFail_whenNotFound() {
            var instanceId = UUID.randomUUID().toString();
            when(serverService.delete(any())).thenReturn(ServiceResult.notFound("not found"));

            var result = service().delete(instanceId);

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

            var result = service().findById(instanceId);

            assertThat(result).isSucceeded().usingRecursiveComparison()
                    .ignoringFields(FIELDS_TO_BE_IGNORED)
                    .isEqualTo(instance);
        }

        @Test
        void shouldReturnNotFound_whenInstanceDoesNotExist() {
            var instanceId = UUID.randomUUID().toString();
            when(serverService.findById(any())).thenReturn(ServiceResult.notFound("not found"));

            var result = service().findById(instanceId);

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        }

    }
}
