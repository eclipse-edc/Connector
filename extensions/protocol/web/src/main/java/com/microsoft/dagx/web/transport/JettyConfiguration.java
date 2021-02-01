package com.microsoft.dagx.web.transport;

/**
 * Provides config values to the Jetty service.
 */
@FunctionalInterface
public interface JettyConfiguration {

    String getSetting(String setting, String defaultValue);
}
