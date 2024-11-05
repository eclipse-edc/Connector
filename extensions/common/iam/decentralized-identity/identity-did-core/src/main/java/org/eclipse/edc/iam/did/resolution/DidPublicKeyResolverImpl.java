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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKParameterNames;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.keys.AbstractPublicKeyResolver;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.eclipse.edc.iam.did.spi.document.DidConstants.ALLOWED_VERIFICATION_TYPES;

public class DidPublicKeyResolverImpl extends AbstractPublicKeyResolver implements DidPublicKeyResolver {

    /**
     * this regex pattern matches both DIDs and DIDs with a fragment (e.g. key-ID).
     * Group 1 ("did")      = the did:method:identifier portion
     * Group 2 ("fragment") = the #fragment portion
     */
    private static final Pattern PATTERN_DID_WITH_OPTIONAL_FRAGMENT = Pattern.compile("(?<did>did:.*:[^#]*)((#)(?<fragment>.*))?");
    private static final String GROUP_DID = "did";
    private static final String GROUP_FRAGMENT = "fragment";
    private final DidResolverRegistry resolverRegistry;

    public DidPublicKeyResolverImpl(KeyParserRegistry registry, DidResolverRegistry resolverRegistry) {
        super(registry);
        this.resolverRegistry = resolverRegistry;
    }

    @Override
    protected Result<String> resolveInternal(String id) {
        if (id == null) {
            return Result.failure("The provided DID is null");
        }

        var matcher = PATTERN_DID_WITH_OPTIONAL_FRAGMENT.matcher(id);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("The given ID must conform to 'did:method:identifier[:fragment]' but did not"); //todo: use Result?
        }

        var did = matcher.group(GROUP_DID);
        String key = null;
        if (matcher.groupCount() > 1) {
            key = matcher.group(GROUP_FRAGMENT);
        }
        return resolveDidPublicKey(did, id, key);
    }

    private Result<String> resolveDidPublicKey(String didUrl, String verificationMethodUrl, @Nullable String keyId) {
        var didResult = resolverRegistry.resolve(didUrl);
        if (didResult.failed()) {
            return didResult.mapFailure();
        }

        var didDocument = didResult.getContent();
        var verificationMethods = validVerificationMethods(didDocument);
        if (verificationMethods.isEmpty()) {
            return Result.failure(format("DID document with id %s does not contain any supported Verification Method", didDocument.getId()));
        }

        // if there are more than 1 verification methods with the same ID
        if (verificationMethods.stream().map(verificationMethodIdMapper(didUrl)).distinct().count() != verificationMethods.size()) {
            return Result.failure("Every verification method must have a unique ID");
        }

        var verificationMethod = selectVerificationMethod(verificationMethods, verificationMethodUrl, keyId);
        return verificationMethod.compose(vm -> {
            var key = new HashMap<>(vm.getPublicKeyJwk());
            key.put(JWKParameterNames.KEY_ID, vm.getId());
            try {
                return Result.success(JWK.parse(key).toJSONString());
            } catch (ParseException e) {
                return Result.failure("Error parsing DID Verification Method: " + e);
            }

        });
    }

    private Result<VerificationMethod> selectVerificationMethod(List<VerificationMethod> verificationMethods, String verificationMethodUrl, @Nullable String keyId) {
        if (keyId == null) { // only valid if exactly 1 verification method
            return verificationMethods.size() == 1 ? Result.success(verificationMethods.get(0)) :
                    Result.failure("The key ID ('kid') is mandatory if DID contains >1 verification methods.");
        }
        // look up VerificationMethods by key ID or didUrl + key ID
        return verificationMethods.stream().filter(vm -> vm.getId().equals(keyId) || vm.getId().equals(verificationMethodUrl))
                .findFirst()
                .map(Result::success)
                .orElseGet(() -> Result.failure("No verification method found with key ID '%s'".formatted(keyId)));
    }

    private List<VerificationMethod> validVerificationMethods(DidDocument didDocument) {
        if (didDocument.getVerificationMethod() == null) {
            return emptyList();
        }
        return didDocument.getVerificationMethod().stream()
                .filter(vm -> ALLOWED_VERIFICATION_TYPES.contains(vm.getType()))
                .toList();
    }

    // If the verification method id is relative uri we map it to didUrl + id
    private Function<VerificationMethod, String> verificationMethodIdMapper(String didUrl) {
        return (vm) -> {
            if (vm.getId().startsWith(didUrl)) {
                return vm.getId();
            } else {
                return didUrl + vm.getId();
            }
        };
    }
}
