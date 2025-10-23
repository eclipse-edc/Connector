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

package org.eclipse.edc.junit.utils;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Holds a set of named endpoints. To configure the default endpoint (web.http.{port,path}), use the `default`
 * string as the name.
 *
 */
public class Endpoints {

    private final Map<String, LazySupplier<URI>> endpoints;


    private Endpoints(Map<String, LazySupplier<URI>> endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Returns the endpoint supplier for the given name.
     *
     * @param name the name of the endpoint
     * @return the endpoint supplier, or null if not found
     */
    @Nullable
    public LazySupplier<URI> getEndpoint(String name) {
        return endpoints.get(name);
    }

    /**
     * Returns all the endpoints.
     *
     * @return a map of endpoint names to their suppliers
     */
    public Map<String, LazySupplier<URI>> getEndpoints() {
        return endpoints;
    }

    public static class Builder {
        private final Map<String, Supplier<URI>> endpoints = new HashMap<>();


        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder endpoint(String name, Supplier<URI> urlSupplier) {
            endpoints.put(name, urlSupplier);
            return this;
        }

        public Endpoints build() {
            var endpoints = this.endpoints.entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), new LazySupplier<>(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            return new Endpoints(endpoints);
        }
    }
}
