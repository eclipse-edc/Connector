package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;
import okhttp3.OkHttpClient;

public class NifiTransferExtension implements ServiceExtension {
    @DagxSetting
    private static final String URL_SETTING = "nifi.url";

    private static final String DEFAULT_NIFI_URL = "http://localhost:8080/nifi-api";

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerManager(context);

        monitor.info("Initialized Core Transfer extension");
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

        NifiDataFlowController manager = new NifiDataFlowController(configuration, context.getTypeManager(), context.getMonitor(), context.getService(Vault.class), httpClient);
        dataFlowManager.register(manager);
    }

}
