package org.eclipse.dataspaceconnector.dataplane.httpproxy;


import org.eclipse.dataspaceconnector.spi.proxy.ProxyEntryHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.token.spi.TokenGenerationService;

public class DataPlaneHttpProxyConsumerExtension implements ServiceExtension {

    @Inject
    private TokenGenerationService tokenGenerationService;
    @Inject
    private ProxyEntryHandlerRegistry proxyEntryHandlerRegistry;

    @Override
    public String name() {
        return "Data Plane Http Proxy Consumer";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var encrypter = new DataAddressEncrypter();
        var handler = new HttpProxyEntryHandler(tokenGenerationService, encrypter, context.getTypeManager());
        proxyEntryHandlerRegistry.put(HttpProxyConstants.HTTP_PROXY_TYPE, handler);
    }
}
