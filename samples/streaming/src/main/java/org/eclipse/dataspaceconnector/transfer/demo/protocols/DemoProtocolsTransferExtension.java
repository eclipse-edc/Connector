package org.eclipse.dataspaceconnector.transfer.demo.protocols;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.common.http.loopback.LoopbackDispatcher;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.http.PubSubHttpEndpoint;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.object.DemoObjectStorage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.object.ObjectStorageFlowController;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.object.ObjectStorageMessage;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.StreamPublisherRegistry;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.TopicManager;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream.message.*;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.stream.*;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.ws.PubSubServerEndpoint;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.ws.WebSocketFactory;
import org.eclipse.dataspaceconnector.web.transport.JettyService;
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
    @EdcSetting
    private static final String WS_PUBSUB_ENDPOINT = "dataspaceconnector.demo.protocol.ws.pubsub";
    private static final String DEFAULT_WS_PUBSUB_ENDPOINT = "ws://localhost:8181/pubsub/";

    @EdcSetting
    private static final String HTTP_PUBSUB_ENDPOINT = "dataspaceconnector.demo.protocol.http.pubsub";
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

        var messageDispatcher = context.getService(RemoteMessageDispatcherRegistry.class);
        var processManager = context.getService(TransferProcessManager.class);

        messageDispatcher.register(new LoopbackDispatcher(processManager, monitor));

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
