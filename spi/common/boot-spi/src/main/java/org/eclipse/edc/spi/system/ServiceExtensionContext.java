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
     * Return the id of the runtime. A runtime is a physical process. If {@code edc.runtime.id} is not configured, a random UUID is used.
     * <br/><em>It is recommended to leave this configuration blank..</em>
     *
     * @return the runtime id.
     */
    String getRuntimeId();

    /**
     * Returns the id of the component. A component is a logical unit of deployment, consisting of 1...N runtimes. If it is not
     * configured, but the runtime ID is configured, then the value of the runtime ID will be used. If neither runtime ID
     * nor component ID are used, (individual) random values are generated.
     * <p>
     * <br/><em>It is recommended to provide a stable value for this configuration.</em>
     *
     * @return the component id.
     */
    String getComponentId();


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
