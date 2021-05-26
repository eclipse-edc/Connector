package com.microsoft.dagx.transfer.demo.protocols;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.transfer.demo.protocols.object.DemoObjectStorage;
import com.microsoft.dagx.transfer.demo.protocols.object.ObjectStorageFlowController;
import com.microsoft.dagx.transfer.demo.protocols.spi.object.ObjectStorageMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.DestinationManager;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamPublisherRegistry;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.ConnectMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.DataMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.PubSubMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.PublishMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.SubscribeMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.UnSubscribeMessage;
import com.microsoft.dagx.transfer.demo.protocols.stream.DemoDestinationManager;
import com.microsoft.dagx.transfer.demo.protocols.stream.PushStreamFlowController;
import com.microsoft.dagx.transfer.demo.protocols.stream.PushStreamProvisioner;
import com.microsoft.dagx.transfer.demo.protocols.stream.PushStreamResourceGenerator;
import com.microsoft.dagx.transfer.demo.protocols.stream.StreamPublisherRegistryImpl;
import com.microsoft.dagx.transfer.demo.protocols.ws.PubSubServerEndpoint;
import com.microsoft.dagx.transfer.demo.protocols.ws.WebSocketFactory;
import com.microsoft.dagx.web.transport.JettyService;

import java.util.Set;

/**
 * An extension that demonstrates data transfers and supports three flow types:
 * <p>
 * (1) Object storage
 * <p>
 * (2) Push-style streaming using pub/sub destinations
 * <p>
 * (3) Pull-style streaming using pub/sub destinations
 * <p>
 * Integration testing
 * <p>
 * The JUnit test for this class demonstrates how to perform extension integration testing using and embedded runtime.
 */
public class DemoProtocolsTransferExtension implements ServiceExtension {
    @DagxSetting
    private static final String WS_PUBSUB_ENDPOINT = "dagx.demo.protocol.ws.pubsub";

    private static final String DEFAULT_WS_PUBSUB_ENDPOINT = "ws://localhost:8181/pubsub/";

    DemoObjectStorage objectStorage;
    DemoDestinationManager destinationManager;

    private Monitor monitor;

    @Override
    public Set<String> provides() {
        return Set.of("demo-protocols");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var objectMapper = context.getTypeManager().getMapper();

        destinationManager = new DemoDestinationManager(monitor);
        context.registerService(DestinationManager.class, destinationManager);

        // setup object storage
        objectStorage = new DemoObjectStorage(monitor);
        context.registerService(DemoObjectStorage.class, objectStorage);

        // setup streaming endpoints
        var jettyService = context.getService(JettyService.class);
        new WebSocketFactory().publishEndpoint(PubSubServerEndpoint.class, () -> new PubSubServerEndpoint(destinationManager, objectMapper, monitor), jettyService);

        registerGenerators(context);

        registerProvisioners(destinationManager, context);

        registerFlowControllers(context, objectMapper);

        registerTypes(objectMapper);
    }

    @Override
    public void start() {
        objectStorage.start();
        destinationManager.start();
    }

    @Override
    public void shutdown() {
        if (objectStorage != null) {
            objectStorage.stop();
        }
        if (destinationManager != null) {
            destinationManager.stop();
        }
    }

    private void registerGenerators(ServiceExtensionContext context) {
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);

        var endpointAddress = context.getSetting(WS_PUBSUB_ENDPOINT, DEFAULT_WS_PUBSUB_ENDPOINT);
        manifestGenerator.registerClientGenerator(new PushStreamResourceGenerator(endpointAddress));
    }

    private void registerProvisioners(DestinationManager destinationManager, ServiceExtensionContext context) {
        var provisionManager = context.getService(ProvisionManager.class);
        provisionManager.register(new PushStreamProvisioner(destinationManager));
    }

    private void registerFlowControllers(ServiceExtensionContext context, ObjectMapper objectMapper) {
        var dataFlowMgr = context.getService(DataFlowManager.class);

        var objectStorageFlowController = new ObjectStorageFlowController(objectMapper, monitor);
        dataFlowMgr.register(objectStorageFlowController);

        var vault = context.getService(Vault.class);
        var publisherRegistry = new StreamPublisherRegistryImpl(vault, objectMapper, monitor);
        context.registerService(StreamPublisherRegistry.class, publisherRegistry);

        var pushStreamFlowController = new PushStreamFlowController(publisherRegistry);
        dataFlowMgr.register(pushStreamFlowController);

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
