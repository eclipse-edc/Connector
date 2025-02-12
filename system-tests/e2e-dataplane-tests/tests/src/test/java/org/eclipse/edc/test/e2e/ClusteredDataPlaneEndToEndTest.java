/*
 *  Copyright (c) 2024 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e;

import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.test.e2e.participant.DataPlaneParticipant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.atLeast;
import static org.mockserver.verify.VerificationTimes.never;

@EndToEndTest
public class ClusteredDataPlaneEndToEndTest {

    @RegisterExtension
    @Order(0)
    private static final PostgresqlStoreSetupExtension POSTGRESQL = new PostgresqlStoreSetupExtension();

    @RegisterExtension
    @Order(1)
    protected static DummyControlPlane dummyControlPlane = new DummyControlPlane();

    private static final DataPlaneParticipant FOO_DATAPLANE = DataPlaneParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();

    private static final DataPlaneParticipant BAR_DATAPLANE = DataPlaneParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();

    private static final BiFunction<String, DataPlaneParticipant, EmbeddedRuntime> RUNTIME_SUPPLIER =
            (name, dataPlaneParticipant) -> new EmbeddedRuntime(
                    name,
                    ":system-tests:e2e-dataplane-tests:runtimes:data-plane",
                    ":dist:bom:dataplane-feature-sql-bom")
                    .configurationProvider(dataPlaneParticipant::dataPlaneConfig)
                    .configurationProvider(POSTGRESQL::getDatasourceConfig)
                    .configurationProvider(dummyControlPlane.dataPlaneConfigurationSupplier())
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                            "edc.runtime.id", name,
                            "edc.sql.schema.autocreate", "true"
                    )));

    private static final EmbeddedRuntime FOO_RUNTIME = RUNTIME_SUPPLIER.apply("foo", FOO_DATAPLANE);
    private static final EmbeddedRuntime BAR_RUNTIME = RUNTIME_SUPPLIER.apply("bar", BAR_DATAPLANE);
    private static final Map<String, EmbeddedRuntime> RUNTIMES = Map.of(
            "foo", FOO_RUNTIME,
            "bar", BAR_RUNTIME
    );

    @RegisterExtension
    private static final RuntimeExtension FOO = new RuntimePerClassExtension(FOO_RUNTIME);

    @RegisterExtension
    private static final RuntimeExtension BAR = new RuntimePerClassExtension(BAR_RUNTIME);

    @Test
    void shouldRestartTransferOwnedByAnotherInstance_whenFlowLeaseExpires() {
        var source = ClientAndServer.startClientAndServer(getFreePort());
        source.when(request()).respond(response("data"));
        var destination = ClientAndServer.startClientAndServer(getFreePort());
        destination.when(request()).respond(response("ok"));

        var sourceAddress = DataAddress.Builder.newInstance().type("PollingHttp")
                .property(EDC_NAMESPACE + "baseUrl", "http://localhost:" + source.getPort())
                .build();

        var destinationAddress = DataAddress.Builder.newInstance().type("HttpData")
                .property(EDC_NAMESPACE + "baseUrl", "http://localhost:" + destination.getPort())
                .build();

        var startMessage = DataFlowStartMessage.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(sourceAddress)
                .destinationDataAddress(destinationAddress)
                .transferType(new TransferType("HttpData", FlowType.PUSH))
                .build();

        var start = runtime().getService(DataPlaneManager.class).start(startMessage);

        assertThat(start).isSucceeded();

        await().untilAsserted(() -> {
            destination.verify(request(), atLeast(1));
        });

        var firstOwnerRuntimeId = runtime().getService(DataPlaneStore.class)
                .findById(startMessage.getProcessId()).getRuntimeId();
        var firstOwner = RUNTIMES.get(firstOwnerRuntimeId);
        firstOwner.shutdown();

        destination.reset();
        destination.verify(request(), never());

        await().untilAsserted(() -> {
            var dataFlow = runtime().getService(DataPlaneStore.class).findById(startMessage.getProcessId());
            assertThat(dataFlow.getRuntimeId()).isNotEqualTo(firstOwner);
            destination.verify(request(), atLeast(1));
        });
    }

    /**
     * Returns a runtime, it does not matter which one.
     *
     * @return a running runtime.
     */
    private @NotNull EmbeddedRuntime runtime() {
        return RUNTIMES.values().stream().filter(EmbeddedRuntime::isRunning).findAny().get();
    }

}
