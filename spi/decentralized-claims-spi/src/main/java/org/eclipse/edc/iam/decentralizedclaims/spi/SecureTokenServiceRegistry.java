/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.spi;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.Nullable;

/**
 * Registry that holds {@link SecureTokenService} implementations bound to a {@code type} string (for example
 * {@code "oauth"} or {@code "embedded"}). Implementors register their {@link SecureTokenService} for a type, and a
 * dispatching {@link SecureTokenService} can then resolve the concrete implementation to use for a given participant
 * context.
 */
@ExtensionPoint
public interface SecureTokenServiceRegistry {

    /**
     * Registers a {@link SecureTokenService} for the given type. Registering a type that already exists overwrites the
     * previously registered implementation.
     *
     * @param type               the type the implementation is bound to, for example {@code "oauth"} or {@code "embedded"}.
     * @param secureTokenService the implementation to bind to the type.
     */
    void register(String type, SecureTokenService secureTokenService);

    /**
     * Resolves the {@link SecureTokenService} bound to the given type.
     *
     * @param type the type to resolve.
     * @return the registered {@link SecureTokenService}, or {@code null} if no implementation is bound to the type.
     */
    @Nullable
    SecureTokenService resolve(String type);

}
