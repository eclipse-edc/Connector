/*
 *  Copyright (c) 2022 Microsoft Corporation
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

/**
 * Interface for custom implementations of {@link ExecutorInstrumentation}.
 * <p>
 * A distinct interface is needed for dependency injection to resolve the optional
 * {@link ExecutorInstrumentationImplementation} vs. the mandatory {@link ExecutorInstrumentation}
 * that is provided by core services, providing a default implementation if none is provided.
 * <p>
 * The Micrometer extension provides a {@link ExecutorInstrumentationImplementation}.
 * The core extension consumes an (optional) {@link ExecutorInstrumentationImplementation}
 * and registers a {@link ExecutorInstrumentation}
 * (with either the {@link ExecutorInstrumentationImplementation} instance or a default implementation).
 * Downstream modules consume a (mandatory) {@link ExecutorInstrumentation}.
 * <p>
 * Because of the way dependencies are resolved (with a graph) it's not possible for
 * the core extension to consume and register the same service.
 */
public interface ExecutorInstrumentationImplementation extends ExecutorInstrumentation {
}