/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.junit.extensions;

import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;

import java.net.URI;

/**
 * Provides access to runtime services and endpoints for a component under test.
 */
public class ComponentRuntimeContext {

    private final String name;
    private final EmbeddedRuntime runtime;
    private final Endpoints endpoints;

    protected ComponentRuntimeContext(String name, EmbeddedRuntime runtime, Endpoints endpoints) {
        this.name = name;
        this.runtime = runtime;
        this.endpoints = endpoints;
    }

    /**
     * Retrieves the endpoint URI supplier for the specified endpoint name.
     *
     * @param name the name of the endpoint
     * @return a lazy supplier of the endpoint URI
     */
    public LazySupplier<URI> getEndpoint(String name) {
        return endpoints.getEndpoint(name);
    }

    /**
     * Retrieves a service instance of the specified class from the runtime.
     *
     * @param klass the class of the service to retrieve
     * @param <T>   the type of the service
     * @return an instance of the requested service
     */
    public <T> T getService(Class<T> klass) {
        return runtime.getService(klass);
    }

    /**
     * Retrieves the configuration of the runtime.
     *
     * @return the runtime configuration
     */
    public Config getConfig() {
        return runtime.getContext().getConfig();
    }

    /**
     * Retrieves the name of the runtime.
     *
     * @return the name of the runtime
     */
    public String getName() {
        return name;
    }
}