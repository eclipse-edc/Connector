/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.junit.extensions;

import org.eclipse.edc.boot.system.runtime.BaseRuntime;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ConfigurationExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.SystemExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Embedded runtime that runs inside another runtime
 */
public class EmbeddedRuntime extends BaseRuntime {

    private final String name;
    private final LinkedHashMap<Class<?>, Object> serviceMocks = new LinkedHashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MultiSourceServiceLocator serviceLocator;
    private final URL[] classPathEntries;
    private Future<?> runtimeThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final List<Supplier<Config>> configurationProviders = new ArrayList<>();

    public EmbeddedRuntime(String name, String... additionalModules) {
        this(new MultiSourceServiceLocator(), name, ClasspathReader.classpathFor(additionalModules));
    }

    public EmbeddedRuntime(String name, URL[] classpathEntries) {
        this(new MultiSourceServiceLocator(), name, classpathEntries);
    }

    private EmbeddedRuntime(MultiSourceServiceLocator serviceLocator, String name, URL[] classPathEntries) {
        super(serviceLocator);
        this.serviceLocator = serviceLocator;
        this.name = name;
        this.classPathEntries = classPathEntries;
    }

    @Override
    public void boot(boolean addShutdownHook) {
        var monitor = super.createMonitor();

        monitor.info("Starting runtime %s".formatted(name));

        configurationProviders.forEach(provider -> serviceLocator
                .registerSystemExtension(ConfigurationExtension.class, (ConfigurationExtension) provider::get));

        var runtimeThrowable = new AtomicReference<Throwable>();
        var latch = new CountDownLatch(1);

        runtimeThread = executorService.submit(() -> {
            try {
                var classLoader = URLClassLoader.newInstance(classPathEntries);

                Thread.currentThread().setContextClassLoader(classLoader);

                super.boot(false);

                latch.countDown();

                isRunning.set(true);
            } catch (Throwable e) {
                runtimeThrowable.set(e);
            }
        });

        try {
            if (!latch.await(30, SECONDS)) {
                throw new EdcException("Failed to start EDC runtime", runtimeThrowable.get());
            }
        } catch (InterruptedException e) {
            throw new EdcException("Failed to start EDC runtime: interrupted", runtimeThrowable.get());
        }

        monitor.info("Runtime %s started".formatted(name));
    }

    @Override
    public void shutdown() {
        serviceLocator.clearSystemExtensions();
        if (isRunning()) {
            super.shutdown();
            isRunning.set(false);
        }
        if (runtimeThread != null && !runtimeThread.isDone()) {
            runtimeThread.cancel(true);
        }
    }

    @Override
    protected @NotNull ServiceExtensionContext createContext(Monitor monitor, Config config) {
        return new TestServiceExtensionContext(monitor, config, serviceMocks);
    }

    @Override
    protected @NotNull Monitor createMonitor() {
        // disable logs when "quiet" log level is set
        if (System.getProperty("org.gradle.logging.level") != null) {
            return new Monitor() {
            };
        } else {
            return new ConsoleMonitor(name, ConsoleMonitor.Level.DEBUG, true);
        }
    }

    public <T extends SystemExtension> EmbeddedRuntime registerSystemExtension(Class<T> type, SystemExtension extension) {
        serviceLocator.registerSystemExtension(type, extension);
        return this;
    }

    public <T> EmbeddedRuntime registerServiceMock(Class<T> type, T mock) {
        serviceMocks.put(type, mock);
        return this;
    }

    public <T> T getService(Class<T> clazz) {
        return context.getService(clazz);
    }

    public ServiceExtensionContext getContext() {
        return context;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Adds a configuration provider, that will be invoked during connector startup.
     *
     * @param configurationProvider the configuration provider.
     * @return self.
     */
    public EmbeddedRuntime configurationProvider(Supplier<Config> configurationProvider) {
        configurationProviders.add(configurationProvider);
        return this;
    }
}
