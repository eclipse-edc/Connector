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

package org.eclipse.edc.iam.did.resolution;

import org.eclipse.edc.iam.did.crypto.key.KeyConverter;
import org.eclipse.edc.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.iam.did.spi.document.DidConstants.ALLOWED_VERIFICATION_TYPES;

public class DidPublicKeyResolverImpl implements DidPublicKeyResolver {
    private final DidResolverRegistry resolverRegistry;

    public DidPublicKeyResolverImpl(DidResolverRegistry resolverRegistry) {
        this.resolverRegistry = resolverRegistry;
    }

    @Override
    public Result<PublicKeyWrapper> resolvePublicKey(String didUrl) {
        var didResult = resolverRegistry.resolve(didUrl);
        if (didResult.failed()) {
            return Result.failure("Invalid DID: " + didResult.getFailureDetail());
        }
        var didDocument = didResult.getContent();
        if (didDocument.getVerificationMethod() == null || didDocument.getVerificationMethod().isEmpty()) {
            return Result.failure("DID does not contain a public key");
        }

        var verificationMethods = didDocument.getVerificationMethod().stream()
                .filter(vm -> ALLOWED_VERIFICATION_TYPES.contains(vm.getType()))
                .toList();
        if (verificationMethods.size() > 1) {
            return Result.failure("DID contains more than one allowed verification type");
        }

        var verificationMethod = didDocument.getVerificationMethod().get(0);
        return KeyConverter.toPublicKeyWrapper(verificationMethod.getPublicKeyJwk(), verificationMethod.getId());
    }

}
