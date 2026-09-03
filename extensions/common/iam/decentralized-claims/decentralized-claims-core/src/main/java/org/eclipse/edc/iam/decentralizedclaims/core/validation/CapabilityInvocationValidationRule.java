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

package org.eclipse.edc.iam.decentralizedclaims.core.validation;

import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Verifies that the signing key (identified by the JWT {@code kid} header) is listed in the
 * {@code capabilityInvocation} section of the issuer's DID document, as required by DCP spec
 * §4.3.3 point 3.
 */
public class CapabilityInvocationValidationRule implements TokenValidationRule {

    private final String keyId;
    private final DidResolverRegistry didResolverRegistry;

    public CapabilityInvocationValidationRule(String keyId, DidResolverRegistry didResolverRegistry) {
        this.keyId = keyId;
        this.didResolverRegistry = didResolverRegistry;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var iss = toVerify.getStringClaim(JwtRegisteredClaimNames.ISSUER);
        if (iss == null) {
            return Result.failure("Required 'iss' claim is missing");
        }

        var didResult = didResolverRegistry.resolve(iss);
        if (didResult.failed()) {
            return didResult.mapEmpty();
        }

        var capabilityInvocations = didResult.getContent().getCapabilityInvocation();
        if (capabilityInvocations.stream().anyMatch(keyId::equals)) {
            return Result.success();
        }
        return Result.failure("The key '%s' is not listed in the 'capabilityInvocation' section of the DID document '%s'".formatted(keyId, iss));
    }
}
