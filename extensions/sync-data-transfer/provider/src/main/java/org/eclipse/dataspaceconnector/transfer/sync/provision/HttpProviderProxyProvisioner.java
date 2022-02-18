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

package org.eclipse.dataspaceconnector.transfer.sync.provision;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.CONTRACT_ID_CLAM;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.DATA_ADDRESS_CLAIM;

public class HttpProviderProxyProvisioner implements Provisioner<HttpProviderProxyResourceDefinition, HttpProviderProxyProvisionedResource> {

    private final String dataProxyAddress;
    private final DataAddressResolver dataAddressResolver;
    private final TokenGenerationService tokenGenerationService;
    private final Long tokenValidityTimeSeconds;
    private final TypeManager typeManager;

    public HttpProviderProxyProvisioner(@NotNull String dataProxyAddress,
                                        @NotNull DataAddressResolver resolver,
                                        @NotNull TokenGenerationService tokenGenerationService,
                                        @NotNull Long tokenValidityTimeSeconds,
                                        @NotNull TypeManager typeManager) {
        this.dataProxyAddress = dataProxyAddress;
        this.dataAddressResolver = resolver;
        this.tokenGenerationService = tokenGenerationService;
        this.tokenValidityTimeSeconds = tokenValidityTimeSeconds;
        this.typeManager = typeManager;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        if (!(resourceDefinition instanceof HttpProviderProxyResourceDefinition)) {
            return false;
        }
        var edrResourceDefinition = (HttpProviderProxyResourceDefinition) resourceDefinition;
        return edrResourceDefinition.isSync();
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return false;
    }

    @Override
    public CompletableFuture<ProvisionResponse> provision(HttpProviderProxyResourceDefinition resourceDefinition) {
        var expiration = Instant.now().plusSeconds(tokenValidityTimeSeconds);
        var dataAddress = dataAddressResolver.resolveForAsset(resourceDefinition.getAssetId());
        var encryptedDataAddress = typeManager.writeValueAsString(dataAddress); // TODO: encrypt data address

        var claims = createClaims(Date.from(expiration), resourceDefinition.getContractId(), encryptedDataAddress);
        var tokenGenerationResult = tokenGenerationService.generate(claims);
        if (tokenGenerationResult.failed()) {
            return CompletableFuture.failedFuture(new EdcException("Failed to generate token"));
        }
        return CompletableFuture.completedFuture(ProvisionResponse.Builder.newInstance()
                .resource(HttpProviderProxyProvisionedResource.Builder.newInstance()
                        .address(dataProxyAddress)
                        .authKey("Authorization")
                        .authCode(tokenGenerationResult.getContent().getToken())
                        .expirationEpochSeconds(expiration.getEpochSecond())
                        .build())
                .build());
    }

    private static Map<String, Object> createClaims(Date expiration, String contractId, String encryptedDataAddress) {
        return Map.of("exp", expiration, CONTRACT_ID_CLAM, contractId, DATA_ADDRESS_CLAIM, encryptedDataAddress);
    }

    @Override
    public CompletableFuture<DeprovisionResponse> deprovision(HttpProviderProxyProvisionedResource provisionedResource) {
        return CompletableFuture.completedFuture(null);
    }
}
