/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.demo.protocols;

import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultResponse;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.DemoProtocols;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.stream.DemoTopicManager;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@ExtendWith(EdcExtension.class)
@Disabled
class DemoProtocolsTransferExtensionTest {

    /**
     * Perform a push stream flow using the loopback protocol.
     *
     * @param processManager     the injected process manager
     * @param destinationManager the injected destination manager
     * @param monitor            the injected runtime monitor
     */
    @Test
    void verifyPushStreamFlow(TransferProcessManager processManager, DemoTopicManager destinationManager, Monitor monitor) throws InterruptedException {
        var latch = new CountDownLatch(1);

        var destinationName = UUID.randomUUID().toString();
        destinationManager.registerObserver((name, payload) -> {
            monitor.info("Message: " + new String(payload));
            latch.countDown();
        });

        var asset = Asset.Builder.newInstance().id("test123").build();

        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("loopback")
                .destinationType(DemoProtocols.PUSH_STREAM_WS)
                .asset(asset)
                .dataDestination(DataAddress.Builder.newInstance().type(DemoProtocols.PUSH_STREAM_WS)
                        .property(DemoProtocols.DESTINATION_NAME, destinationName).build())
                .connectorId("test").build();

        processManager.initiateConsumerRequest(dataRequest);

        latch.await(1, TimeUnit.MINUTES);
    }

    /**
     * Fixture that obtains a reference to the runtime.
     *
     * @param extension the injected runtime instance
     */
    @BeforeEach
    void before(EdcExtension extension) {
        // register a mock Vault
        extension.registerServiceMock(Vault.class, new MockVault());

        // register a wait strategy of 1ms to speed up the interval between transfer manager iterations
        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);
    }

    private static class MockVault implements Vault {
        private final Map<String, String> secrets = new ConcurrentHashMap<>();

        @Override
        public @Nullable String resolveSecret(String key) {
            return secrets.get(key);
        }

        @Override
        public VaultResponse storeSecret(String key, String value) {
            secrets.put(key, value);
            return VaultResponse.OK;
        }

        @Override
        public VaultResponse deleteSecret(String key) {
            secrets.remove(key);
            return VaultResponse.OK;
        }
    }
}
