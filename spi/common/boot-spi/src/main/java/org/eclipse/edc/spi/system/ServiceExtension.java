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

package org.eclipse.edc.spi.system;

/**
 * Contributes services used by the runtime.
 * Service extensions are started after system boostrap.
 */
public interface ServiceExtension extends SystemExtension {

    /**
     * Initializes the extension.
     */
    default void initialize(ServiceExtensionContext context) {
    }

    /**
     * Hook method to perform some additional preparatory work before the extension is started.
     * All dependencies are guaranteed to be resolved, and all other extensions are guaranteed to have completed initialization.
     * <p>
     * Typical use cases include wanting to wait until all registrations of a {@code *Registry} have completed, perform some additional
     * checking whether a service exists or not, etc.
     * <p>
     * <strong>Do NOT perform any service registration in this method!</strong>
     */
    default void prepare() {
    }

    /**
     * Signals the extension to prepare for the runtime to receive requests.
     */
    default void start() {
    }

    /**
     * Signals the extension to release resources and shutdown.
     */
    default void shutdown() {
    }

    /**
     * Do further cleanup of resources that can be still needed during {@link #shutdown()} as database connections, websockets, ...
     */
    default void cleanup() {
    }
}
