/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.proxy;

import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.connector.transfer.dataplane.spi.token.ConsumerPullTokenExpirationDateFunction;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import java.security.PrivateKey;
import java.util.Optional;
import java.util.function.Supplier;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class ConsumerPullDataPlaneProxyResolver {

    private static final String PUBLIC_API_URL_PROPERTY_DEPRECATED = "publicApiUrl";
    private static final String PUBLIC_API_URL_PROPERTY = EDC_NAMESPACE + "publicApiUrl";

    private final DataEncrypter dataEncrypter;
    private final TypeManager typeManager;
    private final TokenGenerationService tokenGenerationService;
    private final Supplier<PrivateKey> keySupplier;
    private final ConsumerPullTokenExpirationDateFunction tokenExpirationDateFunction;

    public ConsumerPullDataPlaneProxyResolver(DataEncrypter dataEncrypter, TypeManager typeManager, TokenGenerationService tokenGenerationService,
                                              Supplier<PrivateKey> keySupplier, ConsumerPullTokenExpirationDateFunction tokenExpirationDateFunction) {
        this.dataEncrypter = dataEncrypter;
        this.typeManager = typeManager;
        this.tokenExpirationDateFunction = tokenExpirationDateFunction;
        this.tokenGenerationService = tokenGenerationService;
        this.keySupplier = keySupplier;
    }

    private static Object getPublicApiUrl(DataPlaneInstance instance) {
        return Optional.ofNullable(instance.getProperties().get(PUBLIC_API_URL_PROPERTY))
                .orElseGet(() -> instance.getProperties().get(PUBLIC_API_URL_PROPERTY_DEPRECATED));
    }

    public Result<DataAddress> toDataAddress(DataRequest request, DataAddress address, DataPlaneInstance instance) {
        return resolveProxyUrl(instance)
                .compose(proxyUrl -> generateAccessToken(address, request.getContractId())
                        .map(token -> DataAddress.Builder.newInstance()
                                .type(EndpointDataReference.EDR_SIMPLE_TYPE)
                                .property(EndpointDataReference.ID, request.getId())
                                .property(EndpointDataReference.CONTRACT_ID, request.getContractId())
                                .property(EndpointDataReference.ENDPOINT, proxyUrl)
                                .property(EndpointDataReference.AUTH_KEY, HttpHeaders.AUTHORIZATION)
                                .property(EndpointDataReference.AUTH_CODE, token)
                                .build()));
    }

    private Result<String> resolveProxyUrl(DataPlaneInstance instance) {
        return Optional.ofNullable(getPublicApiUrl(instance))
                .map(url -> Result.success((String) url))
                .orElse(Result.failure(String.format("Missing property `%s` (deprecated: `%s`) in DataPlaneInstance", PUBLIC_API_URL_PROPERTY, PUBLIC_API_URL_PROPERTY_DEPRECATED)));
    }

    private Result<String> generateAccessToken(DataAddress source, String contractId) {
        var encryptedDataAddress = dataEncrypter.encrypt(typeManager.writeValueAsString(source));
        return tokenExpirationDateFunction.expiresAt(source, contractId)
                .compose(expiration -> tokenGenerationService.generate(keySupplier, new ConsumerPullDataPlaneProxyTokenDecorator(expiration, encryptedDataAddress)))
                .map(TokenRepresentation::getToken);
    }
}
