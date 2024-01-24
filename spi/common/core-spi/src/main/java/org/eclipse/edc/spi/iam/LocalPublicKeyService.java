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

package org.eclipse.edc.spi.iam;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

import java.security.PublicKey;

/**
 * Resolve a public key from an id. This is generic injectable component for resolving public
 * keys configured in the runtime (vault, config, etc.)
 */
@ExtensionPoint
@FunctionalInterface
public interface LocalPublicKeyService {

    /**
     * Resolves the key or return null if not found.
     */
    Result<PublicKey> resolveKey(String id);
}
