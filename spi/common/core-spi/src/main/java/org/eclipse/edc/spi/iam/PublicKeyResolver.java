/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.spi.iam;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;

/**
 * Resolves an RSA public key.
 */
@FunctionalInterface
@ExtensionPoint
public interface PublicKeyResolver {

    /**
     * Resolves the key or return null if not found.
     */
    @Nullable
    PublicKey resolveKey(String id);
}
