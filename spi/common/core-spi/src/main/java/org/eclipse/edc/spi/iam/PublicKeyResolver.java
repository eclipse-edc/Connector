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

import org.eclipse.edc.spi.result.Result;

import java.security.PublicKey;

/**
 * Resolves a public key given an ID. This resolver it's not intended to be used as injectable
 * since multiple {@link PublicKeyResolver} could be needed in the runtime at the same time.
 */
@FunctionalInterface
public interface PublicKeyResolver {

    /**
     * Resolves the key or return null if not found.
     */
    Result<PublicKey> resolveKey(String id);
}
