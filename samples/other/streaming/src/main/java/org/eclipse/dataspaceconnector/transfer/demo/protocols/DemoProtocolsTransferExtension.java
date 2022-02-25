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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.extension.jetty.JettyService;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.transfer.core.inline.InlineDataFlowController;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.dispatcher.LoopbackDispatcher;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.http.PubSubHttpEndpoint;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.object.DemoObjectStorage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.object.ObjectStorageMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.TopicManager;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.ConnectMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.DataMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.PubSubMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.PublishMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.SubscribeMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.UnSubscribeMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.stream.DemoTopicManager;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.stream.PushStreamProvisioner;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.stream.PushStreamResourceGenerator;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.ws.PubSubServerEndpoint;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.ws.WebSocketFactory;

/**
 * An extension that demonstrates data transfers and supports three flow types:
 * (1) Object storage
 * (2) Push-style streaming using pub/sub topics
 * (3) Pull-style streaming using pub/sub topics
 * Integration testing
 * The JUnit test for this class demonstrates how to perform extension integration testing using and embedded runtime.
 */
@Requires({WebService.class, JettyService.class})
public class DemoProtocolsTransferExtension implements ServiceExtension {
    @EdcSetting
    private static final String WS_PUBSUB_ENDPOINT = "edc.demo.protocol.ws.pubsub";
    private static final String DEFAULT_WS_PUBSUB_ENDPOINT = "ws://localhost:8181/pubsub/";

    @EdcSetting
    private static final String HTTP_PUBSUB_ENDPOINT = "edc.demo.protocol.http.pubsub";
    private static final String DEFAULT_HTTP_PUBSUB_ENDPOINT = "http://localhost:8181/api/demo/pubsub/";

    DemoObjectStorage objectStorage;
    DemoTopicManager topicManager;
    PubSubHttpEndpoint httpEndpoint;

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var objectMapper = context.getTypeManager().getMapper();

        topicManager = new DemoTopicManager(monitor);
        context.registerService(TopicManager.class, topicManager);

        // setup object storage
        objectStorage = new DemoObjectStorage(monitor);
        context.registerService(DemoObjectStorage.class, objectStorage);

        // setup streaming endpoints
        var jettyService = context.getService(JettyService.class);
        new WebSocketFactory().publishEndpoint(PubSubServerEndpoint.class, () -> new PubSubServerEndpoint(topicManager, objectMapper, monitor), jettyService);

        httpEndpoint = new PubSubHttpEndpoint(topicManager);
        context.getService(WebService.class).registerResource(httpEndpoint);

        var messageDispatcher = context.getService(RemoteMessageDispatcherRegistry.class);
        var processManager = context.getService(TransferProcessManager.class);

        messageDispatcher.register(new LoopbackDispatcher(processManager, monitor));

        registerGenerators(context);

        registerProvisioners(topicManager, context);

        registerFlowControllers(context);

        registerTypes(objectMapper);
    }

    @Override
    public void start() {
        objectStorage.start();
        topicManager.start();
        httpEndpoint.start();
    }

    @Override
    public void shutdown() {
        if (objectStorage != null) {
            objectStorage.stop();
        }
        if (topicManager != null) {
            topicManager.stop();
        }
        if (httpEndpoint != null) {
            httpEndpoint.stop();
        }
    }

    private void registerGenerators(ServiceExtensionContext context) {
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);

        var wsEndpointAddress = context.getSetting(WS_PUBSUB_ENDPOINT, DEFAULT_WS_PUBSUB_ENDPOINT);
        var httpEndpointAddress = context.getSetting(HTTP_PUBSUB_ENDPOINT, DEFAULT_HTTP_PUBSUB_ENDPOINT);
        manifestGenerator.registerConsumerGenerator(new PushStreamResourceGenerator(wsEndpointAddress, httpEndpointAddress));
    }

    private void registerProvisioners(TopicManager topicManager, ServiceExtensionContext context) {
        var provisionManager = context.getService(ProvisionManager.class);
        provisionManager.register(new PushStreamProvisioner(topicManager));
    }

    private void registerFlowControllers(ServiceExtensionContext context) {
        var dataFlowMgr = context.getService(DataFlowManager.class);

        var vault = context.getService(Vault.class);
        var dataAddressResolver = context.getService(DataAddressResolver.class);

        var dataOperatorRegistry = context.getService(DataOperatorRegistry.class);
        context.registerService(DataOperatorRegistry.class, dataOperatorRegistry);

        var flowController = new InlineDataFlowController(vault, context.getMonitor(), dataOperatorRegistry, dataAddressResolver);
        dataFlowMgr.register(flowController);
    }

    private void registerTypes(ObjectMapper objectMapper) {
        objectMapper.registerSubtypes(ObjectStorageMessage.class,
                ObjectStorageMessage.class,
                PubSubMessage.class,
                SubscribeMessage.class,
                DataMessage.class,
                UnSubscribeMessage.class,
                ConnectMessage.class,
                PublishMessage.class);
    }

}
