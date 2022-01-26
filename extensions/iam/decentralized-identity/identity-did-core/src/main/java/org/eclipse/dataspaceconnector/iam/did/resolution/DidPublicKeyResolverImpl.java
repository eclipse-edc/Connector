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
package org.eclipse.dataspaceconnector.iam.did.resolution;

import org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyConverter;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants.ALLOWED_VERIFICATION_TYPES;

public class DidPublicKeyResolverImpl implements DidPublicKeyResolver {
    private final DidResolverRegistry resolverRegistry;

    public DidPublicKeyResolverImpl(DidResolverRegistry resolverRegistry) {
        this.resolverRegistry = resolverRegistry;
    }

    @Override
    public Result<PublicKeyWrapper> resolvePublicKey(String didUrl) {
        var didResult = resolverRegistry.resolve(didUrl);
        if (didResult.failed()) {
            return Result.failure("Invalid DID: " + String.join(", ", didResult.getFailureMessages()));
        }
        var didDocument = didResult.getContent();
        if (didDocument.getVerificationMethod() == null || didDocument.getVerificationMethod().isEmpty()) {
            return Result.failure("DID does not contain a public key");
        }

        var verificationMethods = didDocument.getVerificationMethod().stream().filter(vm -> ALLOWED_VERIFICATION_TYPES.contains(vm.getType())).collect(Collectors.toList());
        if (verificationMethods.size() > 1) {
            return Result.failure("DID contains more than one allowed verification type");
        }

        var verificationMethod = didDocument.getVerificationMethod().get(0);
        var jwk = verificationMethod.getPublicKeyJwk();
        try {
            return Result.success(KeyConverter.toPublicKeyWrapper(jwk, verificationMethod.getId()));
        } catch (IllegalArgumentException e) {
            return Result.failure("Public key was not a valid EC key. Details: " + e.getMessage());
        }
    }

}
