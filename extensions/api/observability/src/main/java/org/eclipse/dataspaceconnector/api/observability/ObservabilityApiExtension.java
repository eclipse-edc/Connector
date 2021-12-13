package org.eclipse.dataspaceconnector.api.observability;

import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class ObservabilityApiExtension implements ServiceExtension {


    @Override
    public String name() {
        return "EDC Control API";
    }

    @Override
    public Set<String> requires() {
        return Set.of("edc:webservice");
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {

        WebService webService = serviceExtensionContext.getService(WebService.class);

        webService.registerController(new ObservabilityController());

    }

}
