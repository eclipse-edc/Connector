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

package org.eclipse.dataspaceconnector.iam.did.spi.resolution;

import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.spi.result.Result;

/**
 * Delegates to a {@link DidResolver} to resolve a DID document.
 */
public interface DidResolverRegistry {

    /**
     * Registers a DID resolver.
     */
    void register(DidResolver resolver);

    /**
     * Resolves a DID document based on the DID method.
     */
    Result<DidDocument> resolve(String didKey);

}
