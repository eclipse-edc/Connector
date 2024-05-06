/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.spi.system;

import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Context provided to extensions when they are initialized.
 */
public interface ServiceExtensionContext extends SettingResolver {

    String ANONYMOUS_PARTICIPANT = "anonymous";

    /**
     * Freeze the context. It should mark the ServiceExtensionContext as read-only, preventing the registration
     * of new services after the initialization phase
     */
    default void freeze() {

    }

    /**
     * Returns the id of the participant this runtime operates on behalf of. If not configured {@link #ANONYMOUS_PARTICIPANT} is used.
     *
     * @return the participant id.
     */
    String getParticipantId();

    /**
     * Return the id of the runtime. If not configured a random UUID is used.
     *
     * @return the runtime id.
     */
    String getRuntimeId();

    /**
     * Fetches the unique ID of the connector. If the {@code dataspaceconnector.connector.name} config value has been set, that value is returned; otherwise  a random
     * name is chosen.
     *
     * @deprecated use {{@link #getRuntimeId()}} instead.
     */
    @Deprecated(since = "0.6.2")
    default String getConnectorId() {
        return getRuntimeId();
    }

    /**
     * Returns the system monitor.
     */
    default Monitor getMonitor() {
        return getService(Monitor.class);
    }

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
     * Initializes the service context. This should be used to perform tasks like service registrations, etc.
     */
    void initialize();
}
