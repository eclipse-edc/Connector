package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import com.microsoft.dagx.transfer.nifi.aws.S3BucketEndpointConverter;
import com.microsoft.dagx.transfer.nifi.azureblob.AzureEndpointConverter;
import okhttp3.OkHttpClient;

import java.util.Set;

public class NifiTransferExtension implements ServiceExtension {
    @DagxSetting
    private static final String URL_SETTING = "nifi.url";

    private static final String DEFAULT_NIFI_URL = "http://localhost:8080";
    private static final String PROVIDES_NIFI = "nifi";

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerConverters(context);
        registerManager(context);

        monitor.info("Initialized Core Transfer extension");
    }

    private void registerConverters(ServiceExtensionContext context) {
        Vault vault = context.getService(Vault.class);

        EndpointConverterRegistry registry = new EndpointConverterRegistry();
        registry.with(AzureEndpointConverter.TYPE, new AzureEndpointConverter(vault))
                .with(S3BucketEndpointConverter.TYPE, new S3BucketEndpointConverter(vault));
        context.registerService(EndpointConverterRegistry.class, registry);

    }

    @Override
    public Set<String> provides() {
        return Set.of(PROVIDES_NIFI);
    }

    @Override
    public void start() {
        monitor.info("Started Nifi Transfer extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Nifi Transfer extension");
    }

    private void registerManager(ServiceExtensionContext context) {
        DataFlowManager dataFlowManager = context.getService(DataFlowManager.class);

        String url = context.getSetting(URL_SETTING, DEFAULT_NIFI_URL);

        var httpClient = context.getService(OkHttpClient.class);

        NifiTransferManagerConfiguration configuration = NifiTransferManagerConfiguration.Builder.newInstance().url(url).build();

        var converterRegistry= context.getService(EndpointConverterRegistry.class);

        NifiDataFlowController manager = new NifiDataFlowController(configuration, context.getTypeManager(), context.getMonitor(), context.getService(Vault.class), httpClient, converterRegistry);
        dataFlowManager.register(manager);
    }

}
