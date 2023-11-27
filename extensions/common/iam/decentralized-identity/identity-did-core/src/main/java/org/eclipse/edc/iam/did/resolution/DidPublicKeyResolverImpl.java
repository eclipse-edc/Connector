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
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.iam.did.spi.document.DidConstants.ALLOWED_VERIFICATION_TYPES;

public class DidPublicKeyResolverImpl implements DidPublicKeyResolver {
    private final DidResolverRegistry resolverRegistry;

    public DidPublicKeyResolverImpl(DidResolverRegistry resolverRegistry) {
        this.resolverRegistry = resolverRegistry;
    }

    @Override
    public Result<PublicKeyWrapper> resolvePublicKey(String didUrl, @Nullable String keyId) {
        var didResult = resolverRegistry.resolve(didUrl);
        if (didResult.failed()) {
            return didResult.mapTo();
        }
        var didDocument = didResult.getContent();
        if (didDocument.getVerificationMethod() == null || didDocument.getVerificationMethod().isEmpty()) {
            return Result.failure("DID does not contain a public key");
        }

        var verificationMethods = didDocument.getVerificationMethod().stream()
                .filter(vm -> ALLOWED_VERIFICATION_TYPES.contains(vm.getType()))
                .toList();

        // if there are more than 1 verification methods with the same ID
        if (verificationMethods.stream().map(VerificationMethod::getId).distinct().count() != verificationMethods.size()) {
            return Result.failure("Every verification method must have a unique ID");
        }
        Result<VerificationMethod> verificationMethod;

        if (keyId == null) { // only valid if exactly 1 verification method
            if (verificationMethods.size() > 1) {
                return Result.failure("The key ID ('kid') is mandatory if DID contains >1 verification methods.");
            }
            verificationMethod = Result.from(verificationMethods.stream().findFirst());
        } else { // look up VerificationMEthods by key ID
            verificationMethod = verificationMethods.stream().filter(vm -> vm.getId().equals(keyId))
                    .findFirst()
                    .map(Result::success)
                    .orElseGet(() -> Result.failure("No verification method found with key ID '%s'".formatted(keyId)));
        }
        return verificationMethod.compose(vm -> KeyConverter.toPublicKeyWrapper(vm.getPublicKeyJwk(), vm.getId()));
    }

}
