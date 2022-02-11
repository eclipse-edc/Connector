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

package org.eclipse.dataspaceconnector.transfer.demo.protocols.stream;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.fixture.AbstractDemoTransferTest;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.TopicManager;
import org.eclipse.dataspaceconnector.transfer.inline.spi.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.transfer.inline.spi.DataStreamPublisher;
import org.eclipse.dataspaceconnector.transfer.inline.spi.StreamContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.DemoProtocols.DESTINATION_NAME;
import static org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.DemoProtocols.ENDPOINT_ADDRESS;
import static org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.DemoProtocols.PUSH_STREAM_HTTP;
import static org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.DemoProtocols.PUSH_STREAM_WS;

/**
 * Demonstrates an-end-to-end push stream transfer.
 */
class DemoPushStreamTransferTest extends AbstractDemoTransferTest {

    @BeforeAll
    static void setup() {
        //let's randomize the port
        var port = TestUtils.findUnallocatedServerPort();
        System.setProperty("web.http.port", String.valueOf(port));
        System.setProperty("edc.demo.protocol.ws.pubsub", "ws://localhost:" + port + "/pubsub/");
        System.setProperty("edc.demo.protocol.http.pubsub", "http://localhost:" + port + "/api/demo/pubsub/");

    }

    /**
     * Perform a push stream flow over Web Sockets using the loopback protocol.
     *
     * @param processManager the injected process manager
     * @param topicManager   the injected destination manager
     * @param monitor        the injected runtime monitor
     */
    @Test
    void verifyWsPushStreamFlow(TransferProcessManager processManager, TopicManager topicManager, DataOperatorRegistry registry, Monitor monitor, Vault vault, OkHttpClient httpClient, TypeManager typeManager) throws InterruptedException {
        var receiveLatch = new CountDownLatch(1);
        var requestLatch = new CountDownLatch(1);

        var destinationName = UUID.randomUUID().toString();
        topicManager.registerObserver((name, payload) -> {
            monitor.info("Message received: " + new String(payload));
            receiveLatch.countDown();
        });

        var streamPublisher = new TestStreamPublisher(requestLatch);
        streamPublisher.initialize(new PushStreamContext(vault, httpClient, typeManager.getMapper(), monitor));
        registry.registerStreamPublisher(streamPublisher);

        var asset = Asset.Builder.newInstance().id("test123").build();

        var destinationWs = DataAddress.Builder.newInstance().type(PUSH_STREAM_WS).property(DESTINATION_NAME, destinationName).build();
        var dataRequestWs = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("loopback")
                .destinationType(PUSH_STREAM_HTTP)
                .assetId(asset.getId())
                .dataDestination(destinationWs)
                .connectorId("test").build();

        processManager.initiateConsumerRequest(dataRequestWs);

        assertThat(requestLatch.await(1, MINUTES)).isTrue();
        assertThat(receiveLatch.await(1, MINUTES)).isTrue();
    }

    /**
     * Perform a push stream flow over HTTP using the loopback protocol.
     *
     * @param processManager the injected process manager
     * @param topicManager   the injected destination manager
     * @param monitor        the injected runtime monitor
     */
    @Test
    void verifyHttpPushStreamFlow(TransferProcessManager processManager, TopicManager topicManager, DataOperatorRegistry registry, Monitor monitor, Vault vault, OkHttpClient httpClient, TypeManager typeManager) throws InterruptedException {
        var receiveLatch = new CountDownLatch(1);
        var requestLatch = new CountDownLatch(1);

        var destinationName = UUID.randomUUID().toString();
        topicManager.registerObserver((name, payload) -> {
            monitor.info("Message received: " + new String(payload));
            receiveLatch.countDown();
        });

        var streamPublisher = new TestStreamPublisher(requestLatch);
        streamPublisher.initialize(new PushStreamContext(vault, httpClient, typeManager.getMapper(), monitor));
        registry.registerStreamPublisher(streamPublisher);

        var asset = Asset.Builder.newInstance().id("test123").build();

        var destinationHttp = DataAddress.Builder.newInstance().type(PUSH_STREAM_HTTP).property(DESTINATION_NAME, destinationName).build();
        var dataRequestHttp = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("loopback")
                .destinationType(PUSH_STREAM_HTTP)
                .assetId(asset.getId())
                .dataDestination(destinationHttp)
                .connectorId("test").build();

        processManager.initiateConsumerRequest(dataRequestHttp);

        assertThat(requestLatch.await(1, MINUTES)).isTrue();
        assertThat(receiveLatch.await(1, MINUTES)).isTrue();
    }

    private static class TestStreamPublisher implements DataStreamPublisher {
        private final CountDownLatch requestLatch;
        private StreamContext context;

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
        public Result<Void> notifyPublisher(DataRequest dataRequest) {
            var dataAddress = dataRequest.getDataDestination();
            var uriProperty = dataAddress.getProperty(ENDPOINT_ADDRESS);
            var destinationName = dataAddress.getProperty(DESTINATION_NAME);
            var destinationSecretName = dataRequest.getDataDestination().getKeyName();

            try (var session = context.createSession(uriProperty, destinationName, destinationSecretName)) {
                session.publish("test".getBytes());
            }
            requestLatch.countDown();

            return Result.success();
        }
    }
}
