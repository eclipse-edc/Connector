/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.Resource;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ResourceDescriptionRequestHandler extends AbstractDescriptionRequestHandler<OfferedAsset, Resource> {
    private final AssetIndex assetIndex;
    private final ContractOfferService contractOfferService;

    public ResourceDescriptionRequestHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull AssetIndex assetIndex,
            @NotNull ContractOfferService contractOfferService,
            @NotNull TransformerRegistry transformerRegistry) {
        super(
                connectorId,
                monitor,
                transformerRegistry,
                IdsType.RESOURCE,
                Resource.class
        );
        this.assetIndex = Objects.requireNonNull(assetIndex);
        this.contractOfferService = Objects.requireNonNull(contractOfferService);
    }

    protected OfferedAsset retrieveObject(@NotNull IdsId idsId, @NotNull VerificationResult verificationResult) {
        Asset asset = assetIndex.findById(idsId.getValue());
        ContractOfferQuery contractOfferQuery = ContractOfferQuery.Builder.newInstance().targetAsset(asset.getId()).verificationResult(verificationResult).build();
        List<ContractOffer> targetingContractOffers = contractOfferService.queryContractOffers(contractOfferQuery).getContractOfferStream().collect(Collectors.toList());

        return new OfferedAsset(asset, targetingContractOffers);
    }
}
