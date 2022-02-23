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

package org.eclipse.dataspaceconnector.core.executor;

import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;

/**
 * Interface for implementations of {@link ExecutorInstrumentation}s.
 * <p>
 * A distinct interface is provided in order for the dependency injection
 * to provide a default implementation if no other implementation is
 * provided in the classpath.
 * <p>
 * The default implementation does not provide any instrumentation. Extension
 * modules can provide implementations, such as for collecting metrics.
 */
public interface ExecutorInstrumentationImplementation extends ExecutorInstrumentation {
}
