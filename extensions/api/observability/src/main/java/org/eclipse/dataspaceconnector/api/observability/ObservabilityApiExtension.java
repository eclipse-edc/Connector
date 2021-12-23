package org.eclipse.dataspaceconnector.api.observability;

import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;

@Requires({ WebService.class, HealthCheckService.class })
public class ObservabilityApiExtension implements ServiceExtension {

    @Override
    public String name() {
        return "EDC Control API";
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {

        WebService webService = serviceExtensionContext.getService(WebService.class);

        var hcs = serviceExtensionContext.getService(HealthCheckService.class);

        webService.registerController(new ObservabilityApiController(hcs));

        // contribute to the liveness probe
        hcs.addReadinessProvider(() -> HealthCheckResult.Builder.newInstance().component("Observability API").build());
        hcs.addLivenessProvider(() -> HealthCheckResult.Builder.newInstance().component("Observability API").build());
    }

}
