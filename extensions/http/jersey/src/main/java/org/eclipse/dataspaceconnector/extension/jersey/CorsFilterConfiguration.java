package org.eclipse.dataspaceconnector.extension.jersey;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Configuration class to enable or disable CORS and set various settings:
 * <ul>
 *     <li>@code edc.web.rest.cors.enabled} whether the {@link CorsFilter} is registered or not, defaults to {@code Boolean.FALSE.toString()}. So CORS is disabled by default.</li>
 *     <li>@code edc.web.rest.cors.origins} all allowed origins, defaults to {@code "*"}</li>
 *     <li>@code edc.web.rest.cors.headers} all allowed headers, defaults to {@code "origin, content-type, accept, authorization"}</li>
 *     <li>@code edc.web.rest.cors.methods} all allowed methods, defaults to {@code "GET, POST, DELETE, PUT, OPTIONS"}</li>
 * </ul>
 */
public class CorsFilterConfiguration {
    @EdcSetting
    public static final String CORS_CONFIG_ORIGINS_SETTING = "edc.web.rest.cors.origins";
    @EdcSetting
    public static final String CORS_CONFIG_ENABLED_SETTING = "edc.web.rest.cors.enabled";
    @EdcSetting
    public static final String CORS_CONFIG_HEADERS_SETTING = "edc.web.rest.cors.headers";
    @EdcSetting
    public static final String CORS_CONFIG_METHODS_SETTING = "edc.web.rest.cors.methods";
    private String allowedOrigins;
    private String allowedHeaders;
    private String allowedMethods;
    private boolean corsEnabled;

    private CorsFilterConfiguration() {
    }

    public static CorsFilterConfiguration from(ServiceExtensionContext context) {
        var origins = context.getSetting(CORS_CONFIG_ORIGINS_SETTING, "*");
        var headers = context.getSetting(CORS_CONFIG_HEADERS_SETTING, "origin, content-type, accept, authorization");
        var allowedMethods = context.getSetting(CORS_CONFIG_METHODS_SETTING, "GET, POST, DELETE, PUT, OPTIONS");
        var enabled = context.getSetting(CORS_CONFIG_ENABLED_SETTING, Boolean.FALSE.toString());
        var config = new CorsFilterConfiguration();
        config.allowedHeaders = headers;
        config.allowedOrigins = origins;
        config.allowedMethods = allowedMethods;
        config.corsEnabled = Boolean.parseBoolean(enabled);

        return config;
    }

    public static CorsFilterConfiguration none() {
        return new CorsFilterConfiguration();
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }


    public boolean isCorsEnabled() {
        return corsEnabled;
    }
}
