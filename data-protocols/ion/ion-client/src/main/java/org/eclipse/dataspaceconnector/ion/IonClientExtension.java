package org.eclipse.dataspaceconnector.ion;

import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.eclipse.dataspaceconnector.ion.crypto.IonDidPublicKeyResolver;
import org.eclipse.dataspaceconnector.ion.spi.IonClient;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class IonClientExtension implements ServiceExtension {

    private static final String ION_NODE_URL_SETTING = "edc:ion:node:url";
    private static final String DEFAULT_NODE_URL = "https://beta.discover.did.microsoft.com/1.0";

    @Override
    public Set<String> provides() {
        return Set.of(IonClient.FEATURE, DidResolver.FEATURE, DidPublicKeyResolver.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        String ionEndpoint = getIonEndpoint(context);
        context.getMonitor().info("Using ION Node for resolution " + ionEndpoint);
        var client = new DefaultIonClient(ionEndpoint, context.getTypeManager().getMapper());
        context.registerService(IonClient.class, client);
        context.registerService(DidResolver.class, client);

        //registering ION Public Key Resolver
        context.registerService(DidPublicKeyResolver.class, new IonDidPublicKeyResolver(client));
        context.getMonitor().info("Initialized IonClientExtension");
    }

    private String getIonEndpoint(ServiceExtensionContext context) {
        return context.getSetting(ION_NODE_URL_SETTING, DEFAULT_NODE_URL);
    }
}
