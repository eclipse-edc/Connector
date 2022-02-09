package org.eclipse.dataspaceconnector.dataplane.httpproxy;


import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.proxy.DataProxyManager;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.token.spi.TokenGenerationService;

import java.util.concurrent.TimeUnit;

public class DataPlaneHttpProxyProviderExtension implements ServiceExtension {

    @EdcSetting
    private static final String TOKEN_VALIDITY_PERIOD_SETTING = "edc.dataplane.httpproxy.validity-seconds";
    private static final long DEFAULT_TOKEN_VALIDITY_PERIOD_SECONDS = TimeUnit.MINUTES.toSeconds(10);

    @Inject
    private DataProxyManager dataProxyManager;
    @Inject
    private TokenGenerationService tokenGenerationService;

    @Override
    public String name() {
        return "Data Plane Http Proxy Provider";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var tokenValidityPeriodSeconds = context.getSetting(TOKEN_VALIDITY_PERIOD_SETTING, DEFAULT_TOKEN_VALIDITY_PERIOD_SECONDS);
        var encrypter = new DataAddressEncrypter();

        var dataProxy = new RestDataProxy(tokenGenerationService, tokenValidityPeriodSeconds, context.getTypeManager(), encrypter);
        dataProxyManager.addProxy(HttpProxyConstants.HTTP_PROXY_TYPE, dataProxy);
    }
}
