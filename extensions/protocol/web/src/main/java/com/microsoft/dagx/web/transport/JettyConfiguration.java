package com.microsoft.dagx.web.transport;

/**
 * Provides config values to the Jetty service.
 */
@FunctionalInterface
public interface JettyConfiguration {

    <T> T getSetting(String setting, T defaultValue);
}
