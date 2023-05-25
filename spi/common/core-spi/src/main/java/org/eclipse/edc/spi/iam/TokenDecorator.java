/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi.iam;

/**
 * Implementors of this interface receive the {@link org.eclipse.edc.spi.iam.TokenParameters} instance that is composed
 * when the request travels through the egress.
 */
@FunctionalInterface
public interface TokenDecorator {
    /**
     * Callback that allows additions to the {@link TokenParameters} instance that request egress sends out.
     * Be aware that message senders may overwrite protocol-relevant fields such as the audience.
     *
     * @param tokenParametersBuilder A {@link TokenParameters.Builder} with which the desired modifications can be made.
     */
    TokenParameters.Builder decorate(TokenParameters.Builder tokenParametersBuilder);
}
