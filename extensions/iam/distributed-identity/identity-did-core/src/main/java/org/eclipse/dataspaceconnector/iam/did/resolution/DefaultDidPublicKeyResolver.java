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
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;

import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants.ALLOWED_VERIFICATION_TYPES;

public class DefaultDidPublicKeyResolver implements DidPublicKeyResolver {
    private final DidResolverRegistry resolverRegistry;

    public DefaultDidPublicKeyResolver(DidResolverRegistry resolverRegistry) {
        this.resolverRegistry = resolverRegistry;
    }

    @Override
    public DidPublicKeyResolver.Result resolvePublicKey(String didUrl) {
        var didResult = resolverRegistry.resolve(didUrl);
        if (didResult.invalid()) {
            return new Result("Invalid DID: " + didResult.getInvalidMessage());
        }
        var didDocument = didResult.getDidDocument();
        if (didDocument.getVerificationMethod() == null || didDocument.getVerificationMethod().isEmpty()) {
            return new Result("DID does not contain a public key");
        }

        var verificationMethods = didDocument.getVerificationMethod().stream().filter(vm -> ALLOWED_VERIFICATION_TYPES.contains(vm.getType())).collect(Collectors.toList());
        if (verificationMethods.size() > 1) {
            return new Result("DID contains more than one allowed verification type");
        }

        var verificationMethod = didDocument.getVerificationMethod().get(0);
        var jwk = verificationMethod.getPublicKeyJwk();
        try {
            return new Result(KeyConverter.toPublicKeyWrapper(jwk, verificationMethod.getId()));
        } catch (IllegalArgumentException e) {
            return new Result("Public key was not a valid EC key. Details: " + e.getMessage());
        }
    }

}
