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

package org.eclipse.edc.token.spi;

import org.eclipse.edc.spi.iam.TokenParameters;

/**
 * Defines a component that can be used to decorate a JWT token.
 * This functional interface receives both the claims and the headers and is expected to mutate them.
 */
@FunctionalInterface
public interface TokenDecorator {

    /**
     * Decorates the incoming claims and headers. Implementors are free to mutate arbitrarily, although removing is not a common operation.
     *
     * @param tokenParameters Parameters (claims, headers) of the token.
     */
    TokenParameters.Builder decorate(TokenParameters.Builder tokenParameters);
}
