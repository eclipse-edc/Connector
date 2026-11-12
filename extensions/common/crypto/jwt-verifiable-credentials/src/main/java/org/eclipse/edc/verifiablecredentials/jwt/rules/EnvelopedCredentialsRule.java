/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.verifiablecredentials.jwt.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_CREDENTIAL_CONTENT_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ENVELOPED_CREDENTIAL_TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.ID;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.TYPE;
import static org.eclipse.edc.verifiablecredentials.jwt.Constants.VERIFIABLE_CREDENTIAL_JSON_KEY;

/**
 * Validates the credentials of a VCDM 2.0 presentation. The {@code verifiableCredential} claim must be a list of
 * "Enveloped Verifiable Credentials", each of them with an {@code id} that is a semicolon separated list of JWTs, prefixed
 * with {@code data:application/vc+jwt,}, for example:
 * <pre>data:application/vc+jwt,eyAB...;data:application/vc+jwt,eyXYZ...</pre>
 * Every credential JWT is verified with the given function, the rule succeeds only if all of them are valid.
 */
public class EnvelopedCredentialsRule implements TokenValidationRule {

    private final Function<String, Result<Void>> credentialVerifier;

    /**
     * Creates the rule.
     *
     * @param credentialVerifier verifies a single credential JWT, e.g. its signature and its issuer
     */
    public EnvelopedCredentialsRule(Function<String, Result<Void>> credentialVerifier) {
        this.credentialVerifier = credentialVerifier;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        if (!(toVerify.getClaim(VERIFIABLE_CREDENTIAL_JSON_KEY) instanceof List<?> credentials)) {
            return Result.failure("No verifiable credential found in '%s' claim".formatted(VERIFIABLE_CREDENTIAL_JSON_KEY));
        }

        var results = new ArrayList<Result<Void>>();
        for (var credential : credentials) {
            var credentialObject = credential instanceof Map<?, ?> map ? map : Map.of();
            var credentialEnvelopeType = credentialObject.get(TYPE);
            if (!ENVELOPED_CREDENTIAL_TYPE.equals(credentialEnvelopeType)) {
                return Result.failure("Incorrect '%s' field in verifiable credential. Must be '%s' but was '%s'".formatted(TYPE, ENVELOPED_CREDENTIAL_TYPE, credentialEnvelopeType));
            }
            if (!(credentialObject.get(ID) instanceof String credentialEnvelope)) {
                return Result.failure("No enveloped credential found in '%s' claim".formatted(ID));
            }
            results.add(verifyCredentialEnvelope(credentialEnvelope));
        }
        return results.stream().reduce(Result.success(), Result::merge);
    }

    /**
     * Verifies every credential JWT contained in the envelope.
     */
    private Result<Void> verifyCredentialEnvelope(String credentialEnvelope) {
        var results = new ArrayList<Result<Void>>();
        for (var envelope : credentialEnvelope.split(";")) {
            if (!envelope.startsWith(ENVELOPED_CREDENTIAL_CONTENT_TYPE + ",")) {
                return Result.failure("Incorrect credential envelope. Must start with '%s,' but it did not.".formatted(ENVELOPED_CREDENTIAL_CONTENT_TYPE));
            }
            results.add(credentialVerifier.apply(envelope.substring(ENVELOPED_CREDENTIAL_CONTENT_TYPE.length() + 1)));
        }
        return results.stream().reduce(Result.success(), Result::merge);
    }
}
