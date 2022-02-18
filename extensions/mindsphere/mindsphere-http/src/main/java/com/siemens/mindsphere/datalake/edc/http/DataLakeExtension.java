package com.siemens.mindsphere.datalake.edc.http;

import com.siemens.mindsphere.datalake.edc.http.provision.DestinationUrlProvisioner;
import com.siemens.mindsphere.datalake.edc.http.provision.DestinationUrlResourceDefinitionGenerator;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;

import java.net.MalformedURLException;
import java.net.URL;

public class DataLakeExtension implements ServiceExtension {
    @EdcSetting
    private static final String STUB_URL = "edc.demo.http.destination.url";

    @Inject
    private DataOperatorRegistry dataOperatorRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        Monitor monitor = context.getMonitor();
        // create Data Lake client
        final String destinationUrl = context.getSetting(STUB_URL, "http://missing");

        final URL url;
        try {
            url = new URL(destinationUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        final DataLakeClient dataLakeClient = new StubDataLakeClient(null, url);
        // create Data Lake Reader
        final DataLakeReader dataLakeReader = new DataLakeReader(dataLakeClient, monitor);
        // register Data Lake Reader
        dataOperatorRegistry.registerReader(dataLakeReader);

        // register provisioner
        @SuppressWarnings("unchecked") var retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);
        var provisionManager = context.getService(ProvisionManager.class);
        final DestinationUrlProvisioner destinationUrlProvisioner = new DestinationUrlProvisioner(dataLakeClient,
                monitor,
                retryPolicy);
        provisionManager.register(destinationUrlProvisioner);

        // register the URL resource definition generator
        var manifestGenerator = context.getService(ResourceManifestGenerator.class);
        manifestGenerator.registerConsumerGenerator(new DestinationUrlResourceDefinitionGenerator(monitor));

        // register status checker
        var statusCheckerReg = context.getService(StatusCheckerRegistry.class);
        statusCheckerReg.register(HttpSchema.TYPE, new DataLakeStatusChecker(dataLakeClient, retryPolicy, monitor));
    }

    @Override
    public String name() {
        return "MindSphere DataLake";
    }
}
