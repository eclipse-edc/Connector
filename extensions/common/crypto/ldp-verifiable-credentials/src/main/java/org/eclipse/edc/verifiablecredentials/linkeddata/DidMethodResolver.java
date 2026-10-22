/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.verifiablecredentials.linkeddata;

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.ld.DocumentError;
import com.apicatalog.ld.signature.VerificationMethod;
import com.apicatalog.vc.method.resolver.MethodResolver;
import com.apicatalog.vc.proof.Proof;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.EdcException;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * This class implements the MethodResolver interface and is responsible for resolving verification methods for a given DID by
 * delegating to the {@link DidResolverRegistry}.
 */
public class DidMethodResolver implements MethodResolver {
    private final DidResolverRegistry resolverRegistry;

    public DidMethodResolver(DidResolverRegistry resolverRegistry) {
        this.resolverRegistry = resolverRegistry;
    }

    /**
     * Resolves the DID of the given DID URL and returns the verification method of its DID document that has exactly that ID.
     * Relative method IDs (e.g. {@code #key-1}) are resolved against the DID. The method ID must be unique, and a DID URL without
     * a fragment is only accepted if the DID document contains exactly one verification method.
     */
    @Override
    public VerificationMethod resolve(URI id, DocumentLoader documentLoader, Proof proof) throws DocumentError {
        var didUrl = id.toString();
        var fragmentIndex = didUrl.indexOf('#');
        var did = fragmentIndex >= 0 ? didUrl.substring(0, fragmentIndex) : didUrl;

        var didDocument = resolverRegistry.resolve(did)
                .orElseThrow(failure -> new EdcException(failure.getFailureDetail()));

        var verificationMethods = Optional.ofNullable(didDocument.getVerificationMethod()).orElse(List.of());
        var candidates = verificationMethods.stream()
                .filter(vm -> fragmentIndex >= 0 ? didUrl.equals(absoluteId(did, vm.getId())) : absoluteId(did, vm.getId()).startsWith(did + "#"))
                .toList();

        if (candidates.size() != 1) {
            throw new DocumentError(DocumentError.ErrorType.Unknown, "ProofVerificationMethod");
        }

        var verificationMethod = candidates.get(0);
        return new DataIntegrityKeyPair(
                URI.create(absoluteId(did, verificationMethod.getId())),
                URI.create(verificationMethod.getType()),
                URI.create(verificationMethod.getController()),
                verificationMethod.serializePublicKey());
    }

    /**
     * Determines whether the given ID is accepted by checking if it is supported by the resolverRegistry.
     *
     * @param id The ID to check.
     * @return {@code true} if the ID is supported, {@code false} otherwise.
     */
    @Override
    public boolean isAccepted(URI id) {
        return resolverRegistry.isSupported(id.toString());
    }

    private String absoluteId(String did, String verificationMethodId) {
        return verificationMethodId.startsWith("#") ? did + verificationMethodId : verificationMethodId;
    }
}
