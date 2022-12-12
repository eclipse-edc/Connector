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

package org.eclipse.edc.protocol.ids.api.multipart.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.ModelClass;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferQuery;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferResolver;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartRequest;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.service.CatalogService;
import org.eclipse.edc.protocol.ids.spi.service.ConnectorService;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.protocol.ids.spi.types.container.OfferedAsset;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.protocol.ids.api.multipart.util.RequestUtil.getQuerySpec;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.badParameters;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.createMultipartResponse;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.descriptionResponse;
import static org.eclipse.edc.protocol.ids.api.multipart.util.ResponseUtil.notFound;
import static org.eclipse.edc.protocol.ids.spi.types.IdsType.ARTIFACT;
import static org.eclipse.edc.protocol.ids.spi.types.IdsType.CATALOG;
import static org.eclipse.edc.protocol.ids.spi.types.IdsType.CONNECTOR;
import static org.eclipse.edc.protocol.ids.spi.types.IdsType.REPRESENTATION;
import static org.eclipse.edc.protocol.ids.spi.types.IdsType.RESOURCE;

public class DescriptionRequestHandler implements Handler {
    private final Monitor monitor;
    private final IdsId connectorId;
    private final IdsTransformerRegistry transformerRegistry;
    private final AssetIndex assetIndex;
    private final CatalogService catalogService;
    private final ContractOfferResolver contractOfferResolver;
    private final ConnectorService connectorService;
    private final ObjectMapper objectMapper;

    private final Map<IdsType, DescriptionHandler<?>> descriptionHandlers = Map.of(
            ARTIFACT, new ArtifactDescriptionHandler(),
            REPRESENTATION, new RepresentationDescriptionHandler(),
            CONNECTOR, new ConnectorDescriptionHandler(),
            CATALOG, new CatalogDescriptionHandler(),
            RESOURCE, new ResourceDescriptionHandler()
    );

    public DescriptionRequestHandler(
            @NotNull Monitor monitor,
            @NotNull IdsId connectorId,
            @NotNull IdsTransformerRegistry transformerRegistry,
            @NotNull AssetIndex assetIndex,
            @NotNull CatalogService catalogService,
            @NotNull ContractOfferResolver contractOfferResolver,
            @NotNull ConnectorService connectorService,
            @NotNull ObjectMapper objectMapper) {
        this.monitor = monitor;
        this.connectorId = connectorId;
        this.transformerRegistry = transformerRegistry;
        this.assetIndex = assetIndex;
        this.catalogService = catalogService;
        this.contractOfferResolver = contractOfferResolver;
        this.connectorService = connectorService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
        return multipartRequest.getHeader() instanceof DescriptionRequestMessage;
    }

    @Override
    public @NotNull MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest) {
        var message = (DescriptionRequestMessage) multipartRequest.getHeader();

        var idsId = IdsId.from(message.getRequestedElement()).asOptional()
                .orElse(IdsId.Builder.newInstance().type(CONNECTOR).value("fallback").build());

        var descriptionHandler = descriptionHandlers.get(idsId.getType());

        if (descriptionHandler == null) {
            monitor.warning("Cannot handle DescriptionRequest with type " + idsId.getType());
            return createMultipartResponse(notFound(message, connectorId));
        }

        var object = descriptionHandler.getObject(message, idsId, multipartRequest.getClaimToken());
        var result = transformerRegistry.transform(object, descriptionHandler.getType());
        if (result.failed()) {
            monitor.warning(String.format("Could not retrieve requested element with ID %s:%s: [%s]",
                    idsId.getType(), idsId.getValue(), result.getFailureDetail()));

            return createMultipartResponse(badParameters(message, connectorId));
        }

        return createMultipartResponse(descriptionResponse(message, connectorId), result.getContent());
    }

    private interface DescriptionHandler<T extends ModelClass> {
        Class<T> getType();

        Object getObject(DescriptionRequestMessage requestMessage, IdsId idsId, ClaimToken claimToken);
    }

    private class ArtifactDescriptionHandler implements DescriptionHandler<Artifact> {
        @Override
        public Class<Artifact> getType() {
            return Artifact.class;
        }

        @Override
        public Object getObject(DescriptionRequestMessage requestMessage, IdsId idsId, ClaimToken claimToken) {
            return assetIndex.findById(idsId.getValue());
        }
    }

    private class RepresentationDescriptionHandler implements DescriptionHandler<Representation> {
        @Override
        public Class<Representation> getType() {
            return Representation.class;
        }

        @Override
        public Object getObject(DescriptionRequestMessage requestMessage, IdsId idsId, ClaimToken claimToken) {
            return assetIndex.findById(idsId.getValue());
        }
    }

    private class ConnectorDescriptionHandler implements DescriptionHandler<Connector> {
        @Override
        public Class<Connector> getType() {
            return Connector.class;
        }

        @Override
        public Object getObject(DescriptionRequestMessage requestMessage, IdsId idsId, ClaimToken claimToken) {
            return connectorService.getConnector(claimToken, getQuerySpec(requestMessage, objectMapper));
        }
    }

    private class CatalogDescriptionHandler implements DescriptionHandler<ResourceCatalog> {
        @Override
        public Class<ResourceCatalog> getType() {
            return ResourceCatalog.class;
        }

        @Override
        public Object getObject(DescriptionRequestMessage requestMessage, IdsId idsId, ClaimToken claimToken) {
            return catalogService.getDataCatalog(claimToken, getQuerySpec(requestMessage, objectMapper));
        }
    }

    private class ResourceDescriptionHandler implements DescriptionHandler<Resource> {
        @Override
        public Class<Resource> getType() {
            return Resource.class;
        }

        @Override
        public Object getObject(DescriptionRequestMessage requestMessage, IdsId idsId, ClaimToken claimToken) {
            var assetId = idsId.getValue();
            var asset = assetIndex.findById(assetId);
            if (asset == null) {
                monitor.warning(format("Asset with ID %s does not exist.", assetId));
                return null;
            }

            var contractOfferQuery = ContractOfferQuery.Builder.newInstance()
                    .claimToken(claimToken)
                    .assetsCriterion(new Criterion(Asset.PROPERTY_ID, "=", assetId))
                    .build();

            try (var stream = contractOfferResolver.queryContractOffers(contractOfferQuery)) {
                var targetingContractOffers = stream.collect(toList());

                return new OfferedAsset(asset, targetingContractOffers);
            }
        }
    }


}
