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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.ModelClass;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.service.CatalogService;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.MultipartRequestUtil.getInt;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseUtil.badParameters;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseUtil.descriptionResponse;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseUtil.createMultipartResponse;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseUtil.messageTypeNotSupported;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.ResponseUtil.notFound;

public class DescriptionRequestHandler implements Handler {
    private final Monitor monitor;
    private final String connectorId;
    private final IdsTransformerRegistry transformerRegistry;
    private final AssetIndex assetIndex;
    private final CatalogService catalogService;
    private final ContractOfferService contractOfferService;
    private final ConnectorService connectorService;

    public DescriptionRequestHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull IdsTransformerRegistry transformerRegistry,
            @NotNull AssetIndex assetIndex,
            @NotNull CatalogService catalogService,
            @NotNull ContractOfferService contractOfferService,
            @NotNull ConnectorService connectorService) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.assetIndex = Objects.requireNonNull(assetIndex);
        this.catalogService = Objects.requireNonNull(catalogService);
        this.contractOfferService = Objects.requireNonNull(contractOfferService);
        this.connectorService = Objects.requireNonNull(connectorService);
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        Objects.requireNonNull(multipartRequest);

        return multipartRequest.getHeader() instanceof DescriptionRequestMessage;
    }

    @Override
    public MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest,
                                           @NotNull ClaimToken claimToken) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(claimToken);

        try {
            return handleRequestInternal(multipartRequest, claimToken);
        } catch (EdcException exception) {
            monitor.severe(format("Could not handle multipart request: %s", exception.getMessage()), exception);
        }
    
        return createMultipartResponse(messageTypeNotSupported(multipartRequest.getHeader(), connectorId));
    }

    public MultipartResponse handleRequestInternal(@NotNull MultipartRequest multipartRequest,
                                                   @NotNull ClaimToken claimToken) {
        Objects.requireNonNull(multipartRequest);
        Objects.requireNonNull(claimToken);

        var descriptionRequestMessage = (DescriptionRequestMessage) multipartRequest.getHeader();

        var requestedElement = descriptionRequestMessage.getRequestedElement();
        IdsId idsId = null;
        if (requestedElement != null) {
            var idsIdResult = transformerRegistry.transform(requestedElement, IdsId.class);
            if (idsIdResult.failed()) {
                monitor.warning(format("Could not transform URI to IdsId: [%s]",
                        String.join(", ", idsIdResult.getFailureMessages())));
                return createMultipartResponse(badParameters(descriptionRequestMessage, connectorId));
            }

            idsId = idsIdResult.getContent();
        }
    
        //TODO: IDS REFACTORING: this should be a named property of the message object
        // extract paging information, default to 0 ... Integer.MAX_VALUE
        var from = getInt(descriptionRequestMessage, Range.FROM, 0);
        var to = getInt(descriptionRequestMessage, Range.TO, Integer.MAX_VALUE);
        var range = new Range(from, to);
        
        Result<? extends ModelClass> result;
        if (idsId == null || (idsId.getType() == IdsType.CONNECTOR)) {
            result = getConnector(claimToken, range);
        } else {
            var retrievedObject = retrieveRequestedElement(idsId, claimToken, range);
            if (retrievedObject == null) {
                return createMultipartResponse(notFound(descriptionRequestMessage, connectorId));
            }
            result = transformRequestedElement(retrievedObject, idsId.getType());
        }
    
        if (result.failed()) {
            if (idsId == null) {
                monitor.warning(String.format("Could not construct connector description: [%s]",
                        String.join(", ", result.getFailureMessages())));
            } else {
                monitor.warning(String.format("Could not retrieve requested element with ID %s:%s: [%s]",
                        idsId.getType(), idsId.getValue(), String.join(", ", result.getFailureMessages())));
            }
    
            return createMultipartResponse(badParameters(descriptionRequestMessage, connectorId));
        }
        
        var descriptionResponseMessage = descriptionResponse(descriptionRequestMessage, connectorId);
    
        return createMultipartResponse(descriptionResponseMessage, result.getContent());
    }
    
    private Result<Connector> getConnector(ClaimToken claimToken, Range range) {
        return transformerRegistry.transform(connectorService.getConnector(claimToken, range), de.fraunhofer.iais.eis.Connector.class);
    }
    
    private Object retrieveRequestedElement(IdsId idsId, ClaimToken claimToken, Range range) {
        var type = idsId.getType();
        switch (type) {
            case ARTIFACT:
            case REPRESENTATION:
                return assetIndex.findById(idsId.getValue());
            case CATALOG:
                return catalogService.getDataCatalog(claimToken, range);
            case RESOURCE:
                var assetId = idsId.getValue();
                var asset = assetIndex.findById(assetId);
                if (asset == null) {
                    return Result.failure(format("Asset with ID %s does not exist.", assetId));
                }
            
                var contractOfferQuery = ContractOfferQuery.Builder.newInstance()
                        .claimToken(claimToken)
                        .criterion(new Criterion(Asset.PROPERTY_ID, "=", assetId))
                        .build();
                var targetingContractOffers = contractOfferService.queryContractOffers(contractOfferQuery, range).collect(toList());
            
                return new OfferedAsset(asset, targetingContractOffers);
            default:
                return null;
        }
    }
    
    private Result<? extends ModelClass> transformRequestedElement(Object object, IdsType type) {
        switch (type) {
            case ARTIFACT:
                return transformerRegistry.transform(object, Artifact.class);
            case CATALOG:
                return transformerRegistry.transform(object, ResourceCatalog.class);
            case REPRESENTATION:
                return transformerRegistry.transform(object, Representation.class);
            case RESOURCE:
                return transformerRegistry.transform(object, Resource.class);
            default:
                return Result.failure(format("Unknown requested element type: %s", type));
        }
    }
}
