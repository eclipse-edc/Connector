/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.iam.service;

import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * This implementation of the {@link DataPlaneAccessTokenService} uses a backing storage ({@link AccessTokenDataStore}) to keep a record of all
 * tokens it has issued. Tokens are in JWT format.
 */
public class DefaultDataPlaneAccessTokenServiceImpl implements DataPlaneAccessTokenService {
    public static final String TOKEN_ID = "jti";
    private static final List<TokenValidationRule> DATAPLANE_ACCESS_TOKEN_RULES = List.of(
            (claimtoken, additional) -> Objects.equals(claimtoken.getStringClaim("sub"), claimtoken.getStringClaim("iss")) ? Result.success() : Result.failure("'sub' and 'iss' claim must be equal and non-null"),
            (toVerify, additional) -> toVerify.getStringClaim(TOKEN_ID) != null ? Result.success() : Result.failure("%s claim must be present on token".formatted(TOKEN_ID))
    );
    private final TokenGenerationService tokenGenerationService;
    private final AccessTokenDataStore accessTokenDataStore;
    private final Monitor monitor;
    private final Supplier<String> privateKeyIdSupplier;
    private final Supplier<String> publicKeyIdSupplier;
    private final TokenValidationService tokenValidationService;
    private final PublicKeyResolver publicKeyResolver;

    public DefaultDataPlaneAccessTokenServiceImpl(TokenGenerationService tokenGenerationService,
                                                  AccessTokenDataStore accessTokenDataStore,
                                                  Monitor monitor,
                                                  Supplier<String> privateKeyIdSupplier,
                                                  Supplier<String> publicKeyIdSupplier,
                                                  TokenValidationService tokenValidationService,
                                                  PublicKeyResolver publicKeyResolver) {
        this.tokenGenerationService = tokenGenerationService;
        this.accessTokenDataStore = accessTokenDataStore;
        this.monitor = monitor;
        this.privateKeyIdSupplier = privateKeyIdSupplier;
        this.publicKeyIdSupplier = publicKeyIdSupplier;
        this.tokenValidationService = tokenValidationService;
        this.publicKeyResolver = publicKeyResolver;
    }

    /**
     * Generates JWT tokens based on the {@link TokenParameters}. A representation of the claims and the data address is stored for subsequent use, using the token ID ("jti")
     * as correlation id.
     *
     * @param parameters          Headers and claims that are to be included in the token. If the claims do <em>not</em> contain a "jti" claim, one is generated randomly and inserted into the claims.
     * @param backendDataAddress  Information about the data resource for which the token is to be generated. May contain additional information about the token, such as an {@code authType}
     * @param additionalTokenData Additional data that further characterizes the token, but should not be included in the resulting {@link TokenRepresentation}. Will be persisted in the {@link AccessTokenDataStore}.
     * @return A token representation in serialized JWT format (signed). The JWTs "kid" header contains the ID of the public key that can be used to verify the token.
     */
    @Override
    public Result<TokenRepresentation> obtainToken(TokenParameters parameters, DataAddress backendDataAddress, Map<String, Object> additionalTokenData) {
        Objects.requireNonNull(parameters, "TokenParameters must be non-null.");
        Objects.requireNonNull(backendDataAddress, "DataAddress must be non-null.");
        var claimDecorators = parameters.getClaims().entrySet().stream().map(e -> (TokenDecorator) claimDecorator -> claimDecorator.claims(e.getKey(), e.getValue()));
        var headerDecorators = parameters.getHeaders().entrySet().stream().map(e -> (TokenDecorator) headerDecorator -> headerDecorator.header(e.getKey(), e.getValue()));

        var id = parameters.getStringClaim(TOKEN_ID);
        var allDecorators = new ArrayList<>(Stream.concat(claimDecorators, headerDecorators).toList());
        var keyIdDecorator = new KeyIdDecorator(publicKeyIdSupplier.get());
        allDecorators.add(keyIdDecorator);

        // if there is no "jti" header on the token params, we'll assign a random one, and add it back to the decorators
        if (id == null) {
            monitor.debug("No '%s' claim found on TokenParameters. Will generate a random one.".formatted(TOKEN_ID));
            id = UUID.randomUUID().toString();
            var tokenIdDecorator = new TokenIdDecorator(id);
            allDecorators.add(tokenIdDecorator);
        }

        var tokenResult = tokenGenerationService.generate(privateKeyIdSupplier.get(), allDecorators.toArray(new TokenDecorator[0]));
        if (tokenResult.failed()) {
            return tokenResult.mapEmpty();
        }

        // store a record of the token for future reference. We'll need that when we resolve the AccessTokenData later.
        var claimToken = ClaimToken.Builder.newInstance().claims(parameters.getClaims()).build();
        var accessTokenData = new AccessTokenData(id, claimToken, backendDataAddress, additionalTokenData);

        var storeResult = accessTokenDataStore.store(accessTokenData);
        var content = tokenResult.getContent();
        content.getAdditional().put(EDC_NAMESPACE + "authType", "bearer");
        return storeResult.succeeded() ? Result.success(content) : Result.failure(storeResult.getFailureMessages());
    }


    @Override
    public Result<AccessTokenData> resolve(String token) {
        return tokenValidationService.validate(token, publicKeyResolver, DATAPLANE_ACCESS_TOKEN_RULES)
                .map(claimToken -> claimToken.getStringClaim(TOKEN_ID))
                .compose(tokenId -> {
                    var existingAccessToken = accessTokenDataStore.getById(tokenId);

                    return existingAccessToken == null ?
                            Result.failure("AccessTokenData with ID '%s' does not exist.".formatted(tokenId)) :
                            Result.success(existingAccessToken);
                });
    }

    @Override
    public Result<Void> revoke(String transferProcessId, String reason) {

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("additionalProperties.process_id", "=", transferProcessId))
                .build();

        var tokens = accessTokenDataStore.query(query);
        return tokens.stream().map(this::deleteTokenData)
                .reduce(Result::merge)
                .orElseGet(() -> Result.failure("AccessTokenData associated to the transfer with ID '%s' does not exist.".formatted(transferProcessId)));

    }

    private Result<Void> deleteTokenData(AccessTokenData tokenData) {
        var result = accessTokenDataStore.deleteById(tokenData.id());
        if (result.failed()) {
            return Result.failure(result.getFailureDetail());
        } else {
            return Result.success();
        }
    }
}
