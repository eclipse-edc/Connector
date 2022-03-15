/*
 *  Copyright (c) 2022 Amadeus
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

package org.eclipse.dataspaceconnector.transfer.dataplane.sync.provision;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.common.token.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.schema.HttpProxySchema;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.CONTRACT_ID_CLAIM;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.DATA_ADDRESS_CLAIM;

public class HttpProviderProxyProvisioner implements Provisioner<HttpProviderProxyResourceDefinition, HttpProviderProxyProvisionedResource> {

    private final String endpoint;
    private final DataAddressResolver dataAddressResolver;
    private final DataEncrypter dataEncrypter;
    private final TokenGenerationService tokenGenerationService;
    private final Long tokenValidityTimeSeconds;
    private final TypeManager typeManager;

    public HttpProviderProxyProvisioner(String endpoint,
                                        DataAddressResolver resolver,
                                        DataEncrypter dataEncrypter,
                                        TokenGenerationService tokenGenerationService,
                                        Long tokenValidityTimeSeconds,
                                        TypeManager typeManager) {
        this.endpoint = endpoint;
        this.dataAddressResolver = resolver;
        this.dataEncrypter = dataEncrypter;
        this.tokenGenerationService = tokenGenerationService;
        this.tokenValidityTimeSeconds = tokenValidityTimeSeconds;
        this.typeManager = typeManager;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        if (!(resourceDefinition instanceof HttpProviderProxyResourceDefinition)) {
            return false;
        }
        var httpProxyResourceDefinition = (HttpProviderProxyResourceDefinition) resourceDefinition;
        return HttpProxySchema.TYPE.equals(httpProxyResourceDefinition.getType());
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return false;
    }

    @Override
    public CompletableFuture<ProvisionResponse> provision(HttpProviderProxyResourceDefinition resourceDefinition) {
        var expiration = Instant.now().plusSeconds(tokenValidityTimeSeconds);
        var dataAddress = dataAddressResolver.resolveForAsset(resourceDefinition.getAssetId());
        var encryptedDataAddress = dataEncrypter.encrypt(typeManager.writeValueAsString(dataAddress));

        var claims = createClaims(Date.from(expiration), resourceDefinition.getContractId(), encryptedDataAddress);
        var tokenGenerationResult = tokenGenerationService.generate(claims);
        if (tokenGenerationResult.failed()) {
            return CompletableFuture.failedFuture(new EdcException("Failed to generate token"));
        }
        return CompletableFuture.completedFuture(ProvisionResponse.Builder.newInstance()
                .resource(HttpProviderProxyProvisionedResource.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .address(endpoint)
                        .transferProcessId(resourceDefinition.getTransferProcessId())
                        .resourceDefinitionId(resourceDefinition.getId())
                        .token(tokenGenerationResult.getContent().getToken())
                        .expirationEpochSeconds(expiration.getEpochSecond())
                        .build())
                .build());
    }

    private static JWTClaimsSet createClaims(Date expiration, String contractId, String encryptedDataAddress) {
        return new JWTClaimsSet.Builder()
                .expirationTime(expiration)
                .claim(CONTRACT_ID_CLAIM, contractId)
                .claim(DATA_ADDRESS_CLAIM, encryptedDataAddress)
                .build();
    }

    @Override
    public CompletableFuture<DeprovisionResponse> deprovision(HttpProviderProxyProvisionedResource provisionedResource) {
        return CompletableFuture.completedFuture(null);
    }
}
