package org.eclipse.dataspaceconnector.transfer.demo.protocols.stream;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.fixture.AbstractDemoTransferTest;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.StreamContext;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.StreamPublisher;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.StreamPublisherRegistry;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.TopicManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.DemoProtocols.DESTINATION_NAME;
import static org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.DemoProtocols.ENDPOINT_ADDRESS;
import static org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.DemoProtocols.PUSH_STREAM_HTTP;
import static org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.DemoProtocols.PUSH_STREAM_WS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Demonstrates an-end-to-end push stream transfer.
 */
class DemoPushStreamTransferTest extends AbstractDemoTransferTest {

    /**
     * Perform a push stream flow over Web Sockets using the loopback protocol.
     *
     * @param processManager the injected process manager
     * @param topicManager the injected destination manager
     * @param monitor the injected runtime monitor
     */
    @Test
    void verifyWsPushStreamFlow(TransferProcessManager processManager, TopicManager topicManager, StreamPublisherRegistry registry, Monitor monitor) throws InterruptedException {
        var receiveLatch = new CountDownLatch(1);
        var requestLatch = new CountDownLatch(1);

        var destinationName = UUID.randomUUID().toString();
        topicManager.registerObserver((name, payload) -> {
            monitor.info("Message received: " + new String(payload));
            receiveLatch.countDown();
        });

        registry.register(new TestStreamPublisher(requestLatch));

        var dataEntry = DataEntry.Builder.newInstance().id("test123").build();

        var destinationWs = DataAddress.Builder.newInstance().type(PUSH_STREAM_WS).property(DESTINATION_NAME, destinationName).build();
        var dataRequestWs = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("loopback")
                .destinationType(PUSH_STREAM_HTTP)
                .dataEntry(dataEntry)
                .dataDestination(destinationWs)
                .connectorId("test").build();

        processManager.initiateClientRequest(dataRequestWs);

        requestLatch.await(1, MINUTES);
        receiveLatch.await(1, MINUTES);
    }

    /**
     * Perform a push stream flow over HTTP using the loopback protocol.
     *
     * @param processManager the injected process manager
     * @param topicManager the injected destination manager
     * @param monitor the injected runtime monitor
     */
    @Test
    void verifyHttpPushStreamFlow(TransferProcessManager processManager, TopicManager topicManager, StreamPublisherRegistry registry, Monitor monitor) throws InterruptedException {
        var receiveLatch = new CountDownLatch(1);
        var requestLatch = new CountDownLatch(1);

        var destinationName = UUID.randomUUID().toString();
        topicManager.registerObserver((name, payload) -> {
            monitor.info("Message received: " + new String(payload));
            receiveLatch.countDown();
        });

        registry.register(new TestStreamPublisher(requestLatch));

        var dataEntry = DataEntry.Builder.newInstance().id("test123").build();

        var destinationHttp = DataAddress.Builder.newInstance().type(PUSH_STREAM_HTTP).property(DESTINATION_NAME, destinationName).build();
        var dataRequestHttp = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("loopback")
                .destinationType(PUSH_STREAM_HTTP)
                .dataEntry(dataEntry)
                .dataDestination(destinationHttp)
                .connectorId("test").build();

        processManager.initiateClientRequest(dataRequestHttp);

        requestLatch.await(1, MINUTES);
        receiveLatch.await(1, MINUTES);
    }

    private static class TestStreamPublisher implements StreamPublisher {
        private StreamContext context;
        private CountDownLatch requestLatch;

        public TestStreamPublisher(CountDownLatch requestLatch) {
            this.requestLatch = requestLatch;
        }

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
            requestLatch.countDown();
        }
    }
}
