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

import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * Base class for all component extensions. It requires a {@link RuntimePerClassExtension} which means
 * it the runtime will be created once per test class. It takes in input the modules and additional configurations that are required
 * for the runtime to be created. Implementors should provide the default configuration for the runtime.
 * It also supports injecting services mocks tagged with {@link Runtime} in case there are multiple
 * extensions of the same type.
 */
public class ComponentRuntimeExtension extends RuntimePerClassExtension {

    private final ComponentRuntimeContext context;
    protected String name;
    protected Endpoints endpoints = Endpoints.Builder.newInstance().build();
    protected Map<Class<?>, LazySupplier<?>> paramProviders = new HashMap<>();

    private ComponentRuntimeExtension(EmbeddedRuntime runtime, ComponentRuntimeContext context) {
        super(runtime);
        this.context = context;
    }

    public String getName() {
        return name;
    }

    protected Config defaultConfig() {
        var cfg = new HashMap<String, String>();
        endpoints.getEndpoints().forEach((key, endpoint) -> {
            if (key.equals("default")) {
                cfg.put("web.http.port", String.valueOf(endpoint.get().getPort()));
                cfg.put("web.http.path", endpoint.get().getPath());
            } else {
                cfg.put("web.http." + key + ".port", String.valueOf(endpoint.get().getPort()));
                cfg.put("web.http." + key + ".path", endpoint.get().getPath());
            }
        });
        // if the default endpoint is not set, set a random port and /api path
        if (!cfg.containsKey("web.http.port")) {
            cfg.put("web.http.port", String.valueOf(getFreePort()));
            cfg.put("web.http.path", "/api");
        }
        return ConfigFactory.fromMap(cfg);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(ComponentRuntimeContext.class)) {
            return matchName(parameterContext);
        }
        if (paramProviders.containsKey(parameterContext.getParameter().getType())) {
            return matchName(parameterContext);
        }
        return super.supportsParameter(parameterContext, extensionContext) && matchName(parameterContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(ComponentRuntimeContext.class)) {
            return context;
        }
        if (paramProviders.containsKey(parameterContext.getParameter().getType())) {
            return paramProviders.get(parameterContext.getParameter().getType()).get();
        }
        return super.resolveParameter(parameterContext, extensionContext);
    }

    protected boolean matchName(ParameterContext parameterContext) {
        return parameterContext.findAnnotation(Runtime.class)
                .map(Runtime::value)
                .map(name -> name.equals(getName()))
                .orElse(true);
    }

    public static class Builder {

        protected final List<Supplier<Config>> configurationProviders = new ArrayList<>();
        protected final Map<Class<?>, Function<ComponentRuntimeContext, ?>> paramProviders = new HashMap<>();
        protected String name;
        protected List<String> modules = new ArrayList<>();
        protected Endpoints endpoints = Endpoints.Builder.newInstance().build();


        protected Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder endpoints(Endpoints endpoints) {
            this.endpoints = endpoints;
            return this;
        }

        public Builder modules(String... modules) {
            this.modules.addAll(Arrays.stream(modules).toList());
            return this;
        }

        public Builder configurationProvider(Supplier<Config> configurationProvider) {
            this.configurationProviders.add(configurationProvider);
            return this;
        }

        public <T> Builder paramProvider(Class<T> klass, Function<ComponentRuntimeContext, T> paramProvider) {
            this.paramProviders.put(klass, paramProvider);
            return this;
        }

        public ComponentRuntimeExtension build() {
            Objects.requireNonNull(name, "name");

            var runtime = new EmbeddedRuntime(name, modules.toArray(new String[0]));
            var context = new ComponentRuntimeContext(name, runtime, endpoints);
            var extension = new ComponentRuntimeExtension(runtime, context);

            extension.name = name;
            extension.runtime.configurationProvider(extension::defaultConfig);
            extension.endpoints = endpoints;

            extension.paramProviders = paramProviders.entrySet()
                    .stream().map(entry -> Map.entry(entry.getKey(), new LazySupplier<>(() -> entry.getValue().apply(context))))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            configurationProviders.forEach(extension.runtime::configurationProvider);

            return extension;
        }

    }
}