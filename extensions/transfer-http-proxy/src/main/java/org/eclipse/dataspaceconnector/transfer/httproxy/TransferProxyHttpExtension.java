package org.eclipse.dataspaceconnector.transfer.httproxy;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.DataProxyManager;
import org.eclipse.dataspaceconnector.spi.transfer.synchronous.ProxyEntryHandlerRegistry;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class TransferProxyHttpExtension implements ServiceExtension {

    @Override
    public Set<String> requires() {
        return Set.of(DataProxyManager.FEATURE, DataAddressResolver.FEATURE, ProxyEntryHandlerRegistry.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var webService = context.getService(WebService.class);

        var dataAddressResolver = context.getService(DataAddressResolver.class);
        var issuedTokens = new CopyOnWriteArrayList<String>();

        var controller = new ForwardingController(context.getMonitor(), dataAddressResolver, context.getService(Vault.class), context.getService(OkHttpClient.class), issuedTokens);
        webService.registerController(controller);

        var manager = context.getService(DataProxyManager.class);
        manager.addProxy(RestDataProxy.DESTINATION_TYPE_HTTP, new RestDataProxy(controller.getRootPath(), context.getConnectorId(), issuedTokens));

        var registry = context.getService(ProxyEntryHandlerRegistry.class);
        registry.put(RestDataProxy.DESTINATION_TYPE_HTTP, new RestProxyEntryHandler(context.getMonitor()));

    }
}
