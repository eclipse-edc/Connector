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

import java.util.Collection;

/**
 * Holds {@link TokenDecorator}s, that get registered for a particular <em>context</em>. This context determines, under which
 * circumstances a set of decorators should be used, for example when generating tokens for an OAuth2 Server.
 */
public interface TokenDecoratorRegistry {

    /**
     * Adds a {@link TokenDecorator} for a particular context
     */
    void register(String context, TokenDecorator decorator);

    /**
     * Removes a decorator from a specific context. If the same decorator is registered for other contexts also, then {@code unregister()}
     * must be called for each of them.
     * <p>
     * No action is taken if no such mapping exists.
     *
     * @param context   The context from which the decorator is to be removed.
     * @param decorator The decorator to remove.
     */
    void unregister(String context, TokenDecorator decorator);

    /**
     * Returns all token decorators that are registered for a particular context.
     *
     * @param context the context
     * @return A (potentially empty) list, never null
     */
    Collection<TokenDecorator> getDecoratorsFor(String context);
}
