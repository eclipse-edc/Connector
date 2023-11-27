/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.edc.iam.did.spi.resolution;

import org.eclipse.edc.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a public key contained in a DID document associated with a DID.
 */
@ExtensionPoint
public interface DidPublicKeyResolver {

    /**
     * Resolves a public key contained in a DID document.
     *
     * @param did   The DID (Decentralized Identifier) that references a DID document that contains the public key.
     * @param keyId The optional key ID of the public key to resolve. Can <strong>only</strong> be omitted, if the DID document
     *              contains exactly 1 public key. If the key ID is omitted, but the DID document contains >1 public key, an error
     *              is returned.
     * @return A Result object containing a {@link PublicKeyWrapper} if the resolution is successful, or a Failure object if it fails.
     */
    Result<PublicKeyWrapper> resolvePublicKey(String did, @Nullable String keyId);

}
