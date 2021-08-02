package com.microsoft.dagx.transfer.demo.protocols.stream;

import com.microsoft.dagx.spi.monitor.Monitor;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

/**
 *
 */
class DemoTopicManagerTest {
    private DemoTopicManager topicManager;

    @Test
    void verifyPubSub() throws Exception {
        topicManager.start();

        var dataDestination = topicManager.provision("destination").get();

        Consumer<byte[]> consumer1 = EasyMock.createMock(Consumer.class);
        consumer1.accept(EasyMock.isA(byte[].class));
        Consumer<byte[]> consumer2 = EasyMock.createMock(Consumer.class);
        consumer2.accept(EasyMock.isA(byte[].class));

        EasyMock.replay(consumer1, consumer2);

        topicManager.subscribe(dataDestination.getDestinationName(), dataDestination.getAccessToken(), consumer1);
        topicManager.subscribe(dataDestination.getDestinationName(), dataDestination.getAccessToken(), consumer2);

        topicManager.connect("destination",  dataDestination.getAccessToken()).getConsumer().accept("test".getBytes());
        EasyMock.verify(consumer1, consumer2);
    }

    @BeforeEach
    void setUp() {
        topicManager = new DemoTopicManager(new Monitor() {
        });
    }

    @AfterEach
    void tearDown() {
        topicManager.stop();
    }
}
