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

package org.eclipse.dataspaceconnector.iam.did.spi.resolution;

import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.spi.result.Result;

/**
 * Resolves a public key contained in a DID document associated with a DID.
 */
public interface DidPublicKeyResolver {

    String FEATURE = "edc:identity:public-key-resolver";

    /**
     * Resolves the public key.
     */
    Result<PublicKeyWrapper> resolvePublicKey(String did);

}
