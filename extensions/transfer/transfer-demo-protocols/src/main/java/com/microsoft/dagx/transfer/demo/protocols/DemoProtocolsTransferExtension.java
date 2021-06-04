package com.microsoft.dagx.transfer.demo.protocols;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.spi.transfer.provision.ProvisionManager;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.transfer.demo.protocols.http.PubSubHttpEndpoint;
import com.microsoft.dagx.transfer.demo.protocols.object.DemoObjectStorage;
import com.microsoft.dagx.transfer.demo.protocols.object.ObjectStorageFlowController;
import com.microsoft.dagx.transfer.demo.protocols.spi.object.ObjectStorageMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.StreamPublisherRegistry;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.TopicManager;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.ConnectMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.DataMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.PubSubMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.PublishMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.SubscribeMessage;
import com.microsoft.dagx.transfer.demo.protocols.spi.stream.message.UnSubscribeMessage;
import com.microsoft.dagx.transfer.demo.protocols.stream.DemoTopicManager;
import com.microsoft.dagx.transfer.demo.protocols.stream.PushStreamFlowController;
import com.microsoft.dagx.transfer.demo.protocols.stream.PushStreamProvisioner;
import com.microsoft.dagx.transfer.demo.protocols.stream.PushStreamResourceGenerator;
import com.microsoft.dagx.transfer.demo.protocols.stream.StreamPublisherRegistryImpl;
import com.microsoft.dagx.transfer.demo.protocols.ws.PubSubServerEndpoint;
import com.microsoft.dagx.transfer.demo.protocols.ws.WebSocketFactory;
import com.microsoft.dagx.web.transport.JettyService;
import okhttp3.OkHttpClient;

import java.util.Set;

/**
 * An extension that demonstrates data transfers and supports three flow types:
 * <p>
 * (1) Object storage
 * <p>
 * (2) Push-style streaming using pub/sub topics
 * <p>
 * (3) Pull-style streaming using pub/sub topics
 * <p>
 * Integration testing
 * <p>
 * The JUnit test for this class demonstrates how to perform extension integration testing using and embedded runtime.
 */
public class DemoProtocolsTransferExtension implements ServiceExtension {
    @DagxSetting
    private static final String WS_PUBSUB_ENDPOINT = "dagx.demo.protocol.ws.pubsub";
    private static final String DEFAULT_WS_PUBSUB_ENDPOINT = "ws://localhost:8181/pubsub/";

    @DagxSetting
    private static final String HTTP_PUBSUB_ENDPOINT = "dagx.demo.protocol.http.pubsub";
    private static final String DEFAULT_HTTP_PUBSUB_ENDPOINT = "http://localhost:8181/api/demo/pubsub/";

    DemoObjectStorage objectStorage;
    DemoTopicManager topicManager;
    PubSubHttpEndpoint httpEndpoint;

    private Monitor monitor;

    @Override
    public Set<String> provides() {
        return Set.of("demo-protocols");
    }

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
        context.getService(WebService.class).registerController(httpEndpoint);

        registerGenerators(context);

        registerProvisioners(topicManager, context);

        registerFlowControllers(context, objectMapper);

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
        manifestGenerator.registerClientGenerator(new PushStreamResourceGenerator(wsEndpointAddress, httpEndpointAddress));
    }

    private void registerProvisioners(TopicManager topicManager, ServiceExtensionContext context) {
        var provisionManager = context.getService(ProvisionManager.class);
        provisionManager.register(new PushStreamProvisioner(topicManager));
    }

    private void registerFlowControllers(ServiceExtensionContext context, ObjectMapper objectMapper) {
        var dataFlowMgr = context.getService(DataFlowManager.class);

        var objectStorageFlowController = new ObjectStorageFlowController(objectMapper, monitor);
        dataFlowMgr.register(objectStorageFlowController);

        var vault = context.getService(Vault.class);
        var httpClient = context.getService(OkHttpClient.class);

        var publisherRegistry = new StreamPublisherRegistryImpl(vault, httpClient, objectMapper, monitor);
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
