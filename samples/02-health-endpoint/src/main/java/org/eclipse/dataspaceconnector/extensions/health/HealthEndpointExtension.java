package org.eclipse.dataspaceconnector.extensions.health;

import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Requires({WebService.class})
public class HealthEndpointExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        var webService = context.getService(WebService.class);
        webService.registerController(new HealthApiController(context.getMonitor()));
    }
}
