package org.eclipse.dataspaceconnector.extension.jetty;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.ArrayList;

import static java.lang.String.format;

@Provides({ JettyService.class })
public class JettyExtension implements ServiceExtension {

    @EdcSetting
    private static final String HTTP_PORT = "web.http.port";
    @EdcSetting
    private static final String METRICS_PORT = "metrics.http.port";
    @EdcSetting
    private static final String KEYSTORE_PASSWORD = "keystore.password";
    @EdcSetting
    private static final String KEYMANAGER_PASSWORD = "keymanager.password";

    private JettyService jettyService;

    @Override
    public String name() {
        return "Jetty Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var jettyConfiguration = new JettyConfiguration(
                getPort(context, HTTP_PORT),
                context.getSetting(KEYSTORE_PASSWORD, "password"),
                context.getSetting(KEYMANAGER_PASSWORD, "password")
        );

        var internalJettyConfiguration = new JettyConfiguration(
                getPort(context, METRICS_PORT),
                context.getSetting(KEYSTORE_PASSWORD, "password"),
                context.getSetting(KEYMANAGER_PASSWORD, "password")
        );
        ArrayList<JettyConfiguration> jettyConfigurations = new ArrayList<JettyConfiguration>();
        jettyConfigurations.add(jettyConfiguration);
        jettyConfigurations.add(internalJettyConfiguration);

        jettyService = new JettyService(jettyConfigurations, monitor);
        context.registerService(JettyService.class, jettyService);
    }

    private int getPort(ServiceExtensionContext context, String portKey) {
        String portSetting = context.getSetting(portKey, "8181");
        try {
            return Integer.parseInt(portSetting);
        } catch (NumberFormatException e) {
            throw new EdcException(format("Port %s is not a valid number", portSetting));
        }
    }

    @Override
    public void start() {
        jettyService.start();
    }

    @Override
    public void shutdown() {
        if (jettyService != null) {
            jettyService.shutdown();
        }
    }
}
