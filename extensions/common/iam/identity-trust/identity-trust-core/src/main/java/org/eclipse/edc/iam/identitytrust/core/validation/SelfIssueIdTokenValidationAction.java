/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.core.validation;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.identitytrust.spi.validation.TokenValidationAction;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.function.Function;

import static org.eclipse.edc.iam.identitytrust.core.IdentityAndTrustExtension.DCP_SELF_ISSUED_TOKEN_CONTEXT;

public class SelfIssueIdTokenValidationAction implements TokenValidationAction {


    private final TokenValidationService tokenValidationService;
    private final TokenValidationRulesRegistry rulesRegistry;
    private final DidPublicKeyResolver didPublicKeyResolver;
    private final Function<String, String> didResolver;

    public SelfIssueIdTokenValidationAction(TokenValidationService tokenValidationService, TokenValidationRulesRegistry rulesRegistry, DidPublicKeyResolver didPublicKeyResolver, Function<String, String> didResolver) {
        this.tokenValidationService = tokenValidationService;
        this.rulesRegistry = rulesRegistry;
        this.didPublicKeyResolver = didPublicKeyResolver;
        this.didResolver = didResolver;
    }

    @Override
    public Result<ClaimToken> validate(String participantContextId, TokenRepresentation tokenRepresentation) {
        try {
            var signedJwt = SignedJWT.parse(tokenRepresentation.getToken());
            var keyId = signedJwt.getHeader().getKeyID();
            var rules = new ArrayList<>(rulesRegistry.getRules(DCP_SELF_ISSUED_TOKEN_CONTEXT));
            rules.add(new IssuerKeyIdValidationRule(keyId));
            rules.add(new AudienceValidationRule(didResolver.apply(participantContextId)));
            return tokenValidationService.validate(tokenRepresentation, didPublicKeyResolver, rules);
        } catch (ParseException e) {
            throw new EdcException(e);
        }
    }
}
