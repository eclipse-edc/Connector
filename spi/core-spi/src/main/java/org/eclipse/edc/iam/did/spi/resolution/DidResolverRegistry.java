/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.iam.did.spi.resolution;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

/**
 * Delegates to a {@link DidResolver} to resolve a DID document.
 */
@ExtensionPoint
public interface DidResolverRegistry {

    /**
     * Registers a DID resolver.
     */
    void register(DidResolver resolver);

    /**
     * Resolves a DID document based on the DID method.
     */
    Result<DidDocument> resolve(String didKey);

    /**
     * Checks if the given DID ID is supported.
     *
     * @param didKey The DID key to check. This string must contain the "did" keyword, followed by the DID method, followed by the identifier, separated by a ":".
     * @return {@code true} if the given DID key is supported, {@code false} otherwise.
     */
    boolean isSupported(String didKey);
}
