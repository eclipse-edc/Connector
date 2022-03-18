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
 *       Fraunhofer Institute for Software and Systems Engineering
 *
 */
package org.eclipse.dataspaceconnector.identity;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.credentials.VerifiableCredentialFactory;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyConverter;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidDocument;
import org.eclipse.dataspaceconnector.iam.did.spi.document.JwkPublicKey;
import org.eclipse.dataspaceconnector.iam.did.spi.document.Service;
import org.eclipse.dataspaceconnector.iam.did.spi.document.VerificationMethod;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class DecentralizedIdentityService implements IdentityService {
    private final Supplier<SignedJWT> verifiableCredentialProvider;
    private final DidResolverRegistry resolverRegistry;
    private final CredentialsVerifier credentialsVerifier;
    private final Monitor monitor;

    public DecentralizedIdentityService(Supplier<SignedJWT> vcProvider, DidResolverRegistry resolverRegistry, CredentialsVerifier credentialsVerifier, Monitor monitor) {
        verifiableCredentialProvider = vcProvider;
        this.resolverRegistry = resolverRegistry;
        this.credentialsVerifier = credentialsVerifier;
        this.monitor = monitor;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(String scope) {

        var jwt = verifiableCredentialProvider.get();
        var token = jwt.serialize();
        var expiration = new Date().getTime() + TimeUnit.MINUTES.toMillis(10);

        return Result.success(TokenRepresentation.Builder.newInstance().token(token).expiresIn(expiration).build());
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation) {
        try {
            var jwt = SignedJWT.parse(tokenRepresentation.getToken());
            monitor.debug("Starting verification...");

            monitor.debug("Resolving other party's DID Document");
            var didResult = resolverRegistry.resolve(jwt.getJWTClaimsSet().getIssuer());
            if (didResult.failed()) {
                return Result.failure("Unable to resolve DID: " + String.join(", ", didResult.getFailureMessages()));
            }
            monitor.debug("Extracting public key");

            // this will return the _first_ public key entry
            Optional<VerificationMethod> publicKey = getPublicKey(didResult.getContent());
            if (publicKey.isEmpty()) {
                return Result.failure("Public Key not found in DID Document!");
            }

            //convert the POJO into a usable PK-wrapper:
            JwkPublicKey publicKeyJwk = publicKey.get().getPublicKeyJwk();
            PublicKeyWrapper publicKeyWrapper = KeyConverter.toPublicKeyWrapper(publicKeyJwk, publicKey.get().getId());

            monitor.debug("Verifying JWT with public key...");
            if (!VerifiableCredentialFactory.verify(jwt, publicKeyWrapper)) {
                return Result.failure("Token could not be verified!");
            }
            monitor.debug("verification successful! Fetching data from IdentityHub");
            String hubUrl = getHubUrl(didResult.getContent());
            var credentialsResult = credentialsVerifier.verifyCredentials(hubUrl, publicKeyWrapper);

            monitor.debug("Building ClaimToken");
            var tokenBuilder = ClaimToken.Builder.newInstance();
            var claimToken = tokenBuilder.claims(credentialsResult.getContent()).build();

            return Result.success(claimToken);
        } catch (ParseException e) {
            monitor.info("Error parsing JWT", e);
            return Result.failure("Error parsing JWT");
        }
    }

    String getHubUrl(DidDocument did) {
        return did.getService().stream().filter(service -> service.getType().equals(DidConstants.HUB_URL)).map(Service::getServiceEndpoint).findFirst().orElseThrow();
    }

    @NotNull
    private Optional<VerificationMethod> getPublicKey(DidDocument did) {
        return did.getVerificationMethod().stream().filter(vm -> DidConstants.ALLOWED_VERIFICATION_TYPES.contains(vm.getType())).findFirst();
    }
}
