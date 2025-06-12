/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.spi.context;

import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Provides a protocol request context provider
 *
 * @param <I> the input type.
 * @param <C> the context type.
 */
public interface ProtocolRequestContextProvider<I, C extends ProtocolRequestContext> {

    /**
     * Returns the protocol request context provider given the input.
     *
     * @param input the input.
     * @return the protocol request context.
     */
    ServiceResult<C> provide(I input);

}
