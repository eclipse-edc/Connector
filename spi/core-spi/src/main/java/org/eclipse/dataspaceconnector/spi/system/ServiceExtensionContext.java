/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.system;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Context provided to extensions when they are initialized.
 */
public interface ServiceExtensionContext extends SettingResolver {

    /**
     * Fetches the unique ID of the connector. If the {@code dataspaceconnector.connector.name} config value has been set, that value is returned; otherwise  a random
     * name is chosen.
     */
    String getConnectorId();

    /**
     * Returns the system monitor.
     */
    Monitor getMonitor();

    /**
     * Returns the type manager.
     */
    TypeManager getTypeManager();

    /**
     * Returns true if the service type is registered.
     */
    <T> boolean hasService(Class<T> type);

    /**
     * Returns a system service.
     */
    <T> T getService(Class<T> type);

    /**
     * Returns a system service, but does not throw an exception if not found.
     *
     * @return null if not found
     */
    default <T> T getService(Class<T> type, boolean isOptional) {
        return getService(type);
    }

    /**
     * Registers a service
     */
    default <T> void registerService(Class<T> type, T service) {
    }

    /**
     * Loads and orders the service extensions.
     */
    List<InjectionContainer<ServiceExtension>> loadServiceExtensions();

    /**
     * Loads multiple extensions, raising an exception if at least one is not found.
     */
    <T> List<T> loadExtensions(Class<T> type, boolean required);

    /**
     * Loads a single extension, raising an exception if one is not found.
     */
    @Nullable()
    @Contract("_, true -> !null")
    <T> T loadSingletonExtension(Class<T> type, boolean required);

    /**
     * Initializes the service context. This should be used to perform tasks like service registrations, etc.
     */
    void initialize();
}
