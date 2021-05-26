package com.microsoft.dagx.transfer.demo.protocols;

import com.microsoft.dagx.junit.DagxExtension;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.security.VaultResponse;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.transfer.TransferWaitStrategy;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.transfer.demo.protocols.spi.DemoProtocols;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.DestinationManager;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamContext;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamPublisher;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamPublisherRegistry;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.microsoft.dagx.transfer.demo.protocols.spi.DemoProtocols.DESTINATION_NAME;
import static com.microsoft.dagx.transfer.demo.protocols.spi.DemoProtocols.ENDPOINT_ADDRESS;

/**
 *
 */
@ExtendWith(DagxExtension.class)
//@Disabled
class DemoProtocolsTransferExtensionTest {

    /**
     * Perform a push stream flow using the loopback protocol.
     *
     * @param processManager the injected process manager
     * @param destinationManager the injected destination manager
     * @param monitor the injected runtime monitor
     */
    @Test
    void verifyPushStreamFlow(TransferProcessManager processManager, DestinationManager destinationManager, StreamPublisherRegistry registry, Monitor monitor) throws InterruptedException {
        var latch = new CountDownLatch(1);

        var destinationName = UUID.randomUUID().toString();
        destinationManager.registerObserver((name, payload) -> {
            monitor.info("Message: " + new String(payload));
            latch.countDown();
        });

        registry.register(new TestStreamPublisher());

        var dataEntry = DataEntry.Builder.newInstance().id("test123").build();

        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("loopback")
                .destinationType(DemoProtocols.PUSH_STREAM)
                .dataEntry(dataEntry)
                .dataDestination(DataAddress.Builder.newInstance().type(DemoProtocols.PUSH_STREAM)
                        .property(DemoProtocols.DESTINATION_NAME, destinationName).build())
                .connectorId("test").build();

        processManager.initiateClientRequest(dataRequest);

        latch.await(1, TimeUnit.MINUTES);
    }

    /**
     * Fixture that obtains a reference to the runtime.
     *
     * @param extension the injected runtime instance
     */
    @BeforeEach
    void before(DagxExtension extension) {
        // register a mock Vault
        extension.registerServiceMock(Vault.class, new MockVault());

        // register a wait strategy of 1ms to speed up the interval between transfer manager iterations
        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);
    }

    private static class MockVault implements Vault {
        private Map<String, String> secrets = new ConcurrentHashMap<>();

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

    private static class TestStreamPublisher implements StreamPublisher {
        private StreamContext context;

        @Override
        public void initialize(StreamContext context) {
            this.context = context;
        }

        @Override
        public boolean canHandle(DataRequest dataRequest) {
            return true;
        }

        @Override
        public void notifyPublisher(DataRequest dataRequest) {
            var dataAddress = dataRequest.getDataDestination();
            var uriProperty = dataAddress.getProperty(ENDPOINT_ADDRESS);
            //        // TODO handle nulls
            var destinationName = dataAddress.getProperty(DESTINATION_NAME);
            var destinationSecretName = dataRequest.getDataDestination().getKeyName();

            try (var session = context.createSession(uriProperty, destinationName, destinationSecretName)) {
                session.publish("test".getBytes());
//                session.publish("test".getBytes());
            }

        }
    }
}
