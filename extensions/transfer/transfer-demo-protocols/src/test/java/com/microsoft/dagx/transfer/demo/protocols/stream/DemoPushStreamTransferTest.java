package com.microsoft.dagx.transfer.demo.protocols.stream;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.transfer.demo.protocols.fixture.AbstractDemoTransferTest;
import com.microsoft.dagx.transfer.demo.protocols.spi.DemoProtocols;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.DestinationManager;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamContext;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamPublisher;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamPublisherRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import static java.util.concurrent.TimeUnit.MINUTES;

import static com.microsoft.dagx.transfer.demo.protocols.spi.DemoProtocols.DESTINATION_NAME;
import static com.microsoft.dagx.transfer.demo.protocols.spi.DemoProtocols.ENDPOINT_ADDRESS;

/**
 * Demonstrates an-end-to-end push stream transfer.
 */
class DemoPushStreamTransferTest extends AbstractDemoTransferTest {

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

        latch.await(1, MINUTES);
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
            var destinationName = dataAddress.getProperty(DESTINATION_NAME);
            var destinationSecretName = dataRequest.getDataDestination().getKeyName();

            try (var session = context.createSession(uriProperty, destinationName, destinationSecretName)) {
                session.publish("test".getBytes());
            }

        }
    }
}
