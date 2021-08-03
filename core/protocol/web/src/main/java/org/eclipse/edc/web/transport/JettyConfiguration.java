/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.web.transport;

/**
 * Provides config values to the Jetty service.
 */
@FunctionalInterface
public interface JettyConfiguration {

    String getSetting(String setting, String defaultValue);
}
