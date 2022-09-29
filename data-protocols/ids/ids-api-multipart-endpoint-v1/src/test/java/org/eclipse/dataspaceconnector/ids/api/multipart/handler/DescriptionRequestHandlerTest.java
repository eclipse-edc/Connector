/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.ArtifactBuilder;
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.BaseConnectorBuilder;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DescriptionRequestMessageImpl;
import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceBuilder;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.ResourceCatalogBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.spi.domain.connector.Connector;
import org.eclipse.dataspaceconnector.ids.spi.service.CatalogService;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.types.IdsType;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DescriptionRequestHandlerTest {

    private static final String CONNECTOR_ID = "urn:connector:edc";

    private final int rangeFrom = 0;
    private final int rangeTo = 10;

    private static final String PROPERTY = "property";
    private static final String VALUE = "value";
    private static final String EQUALS_SIGN = "=";

    private DescriptionRequestHandler handler;

    private IdsTransformerRegistry transformerRegistry;
    private AssetIndex assetIndex;
    private CatalogService catalogService;
    private ContractOfferService contractOfferService;
    private ConnectorService connectorService;

    @BeforeEach
    void init() {
        transformerRegistry = mock(IdsTransformerRegistry.class);
        assetIndex = mock(AssetIndex.class);
        catalogService = mock(CatalogService.class);
        contractOfferService = mock(ContractOfferService.class);
        connectorService = mock(ConnectorService.class);

        handler = new DescriptionRequestHandler(mock(Monitor.class), CONNECTOR_ID, transformerRegistry,
                assetIndex, catalogService, contractOfferService, connectorService, new ObjectMapper());
    }

    @Test
    void canHandle_messageTypeSupported_returnTrue() {
        var request = MultipartRequest.Builder.newInstance()
                .header(new DescriptionRequestMessageBuilder().build())
                .claimToken(ClaimToken.Builder.newInstance().build())
                .build();

        assertThat(handler.canHandle(request)).isTrue();
    }

    @Test
    void canHandle_messageTypeNotSupported_returnFalse() {
        var request = MultipartRequest.Builder.newInstance()
                .header(new ArtifactRequestMessageBuilder().build())
                .claimToken(ClaimToken.Builder.newInstance().build())
                .build();

        assertThat(handler.canHandle(request)).isFalse();
    }

    @Test
    void handleRequest_connector_returnDescription() {
        var connector = Connector.Builder.newInstance().build();
        var idsConnector = new BaseConnectorBuilder().build();
        var request = MultipartRequest.Builder.newInstance()
                .header(descriptionRequestMessage(null))
                .claimToken(ClaimToken.Builder.newInstance().build())
                .build();

        when(connectorService.getConnector(any(), any())).thenReturn(connector);
        when(transformerRegistry.transform(connector, de.fraunhofer.iais.eis.Connector.class)).thenReturn(Result.success(idsConnector));

        var response = handler.handleRequest(request);

        assertThat(response.getHeader()).isNotNull().isInstanceOf(DescriptionResponseMessage.class);
        assertThat(response.getPayload()).isNotNull().isEqualTo(idsConnector);

        verify(connectorService, times(1))
                .getConnector(any(), argThat(query -> query.getRange().getFrom() == rangeFrom && query.getRange().getTo() == rangeTo));
        verifyNoMoreInteractions(connectorService);
        verifyNoInteractions(catalogService, contractOfferService, assetIndex);
    }

    @Test
    void handleRequest_catalog_returnDescription() {
        var catalog = Catalog.Builder.newInstance().id("1").contractOffers(new ArrayList<>()).build();
        var idsCatalog = new ResourceCatalogBuilder().build();
        var request = MultipartRequest.Builder.newInstance()
                .header(descriptionRequestMessage(URI.create("urn:catalog:1")))
                .claimToken(ClaimToken.Builder.newInstance().build())
                .build();

        when(catalogService.getDataCatalog(any(), any())).thenReturn(catalog);
        when(transformerRegistry.transform(catalog, ResourceCatalog.class)).thenReturn(Result.success(idsCatalog));

        var response = handler.handleRequest(request);

        assertThat(response.getHeader()).isNotNull().isInstanceOf(DescriptionResponseMessage.class);
        assertThat(response.getPayload()).isNotNull().isEqualTo(idsCatalog);

        verify(catalogService, times(1))
                .getDataCatalog(any(),
                    argThat(query -> query.getRange().getFrom() == rangeFrom && query.getRange().getTo() == rangeTo &&
                        query.getFilterExpression().get(0).getOperandLeft().equals(PROPERTY) &&
                        query.getFilterExpression().get(0).getOperandRight().equals(VALUE) &&
                        query.getFilterExpression().get(0).getOperator().equals(EQUALS_SIGN)));
        verifyNoMoreInteractions(catalogService);
        verifyNoInteractions(connectorService, contractOfferService, assetIndex);
    }

    @Test
    void handleRequest_resource_returnDescription() {
        var assetId = "1";

        var asset = Asset.Builder.newInstance().id(assetId).build();
        var idsResource = new ResourceBuilder().build();
        var contractOffer = ContractOffer.Builder.newInstance()
                .id("id")
                .policy(Policy.Builder.newInstance().build())
                .build();
        var request = MultipartRequest.Builder.newInstance()
                .header(descriptionRequestMessage(URI.create("urn:resource:" + assetId)))
                .claimToken(ClaimToken.Builder.newInstance().build())
                .build();

        when(assetIndex.findById(any())).thenReturn(asset);
        when(contractOfferService.queryContractOffers(any())).thenReturn(Stream.of(contractOffer));
        when(transformerRegistry.transform(any(), eq(Resource.class))).thenReturn(Result.success(idsResource));

        var response = handler.handleRequest(request);

        assertThat(response.getHeader()).isNotNull().isInstanceOf(DescriptionResponseMessage.class);
        assertThat(response.getPayload()).isNotNull().isEqualTo(idsResource);

        verify(assetIndex, times(1)).findById(assetId);
        verifyNoMoreInteractions(assetIndex);
        verify(contractOfferService, times(1)).queryContractOffers(any());
        verifyNoMoreInteractions(contractOfferService);
        verifyNoMoreInteractions(connectorService, catalogService);
    }

    @Test
    void handleRequest_representation_returnDescription() {
        var assetId = "1";

        var asset = Asset.Builder.newInstance().id(assetId).build();
        var idsRepresentation = new RepresentationBuilder().build();
        var idsId = IdsId.Builder.newInstance()
                .type(IdsType.REPRESENTATION)
                .value(assetId)
                .build();
        var request = MultipartRequest.Builder.newInstance()
                .header(descriptionRequestMessage(URI.create("urn:representation:" + assetId)))
                .claimToken(ClaimToken.Builder.newInstance().build())
                .build();

        when(assetIndex.findById(any())).thenReturn(asset);
        when(transformerRegistry.transform(any(), eq(IdsId.class))).thenReturn(Result.success(idsId));
        when(transformerRegistry.transform(asset, Representation.class)).thenReturn(Result.success(idsRepresentation));

        var response = handler.handleRequest(request);

        assertThat(response.getHeader()).isNotNull().isInstanceOf(DescriptionResponseMessage.class);
        assertThat(response.getPayload()).isNotNull().isEqualTo(idsRepresentation);

        verify(assetIndex, times(1)).findById(assetId);
        verifyNoMoreInteractions(assetIndex);
        verifyNoInteractions(connectorService, catalogService, contractOfferService);
    }

    @Test
    void handleRequest_artifact_returnDescription() {
        var assetId = "1";

        var asset = Asset.Builder.newInstance().id(assetId).build();
        var idsArtifact = new ArtifactBuilder().build();
        var idsId = IdsId.Builder.newInstance()
                .type(IdsType.ARTIFACT)
                .value(assetId)
                .build();
        var request = MultipartRequest.Builder.newInstance()
                .header(descriptionRequestMessage(URI.create("urn:artifact:" + assetId)))
                .claimToken(ClaimToken.Builder.newInstance().build())
                .build();

        when(assetIndex.findById(any())).thenReturn(asset);
        when(transformerRegistry.transform(any(), eq(IdsId.class))).thenReturn(Result.success(idsId));
        when(transformerRegistry.transform(asset, Artifact.class)).thenReturn(Result.success(idsArtifact));

        var response = handler.handleRequest(request);

        assertThat(response.getHeader()).isNotNull().isInstanceOf(DescriptionResponseMessage.class);
        assertThat(response.getPayload()).isNotNull().isEqualTo(idsArtifact);

        verify(assetIndex, times(1)).findById(assetId);
        verifyNoMoreInteractions(assetIndex);
        verifyNoInteractions(connectorService, catalogService, contractOfferService);
    }

    private DescriptionRequestMessage descriptionRequestMessage(URI requestedElement) {
        var message = (DescriptionRequestMessageImpl) new DescriptionRequestMessageBuilder()
                ._senderAgent_(URI.create("senderAgent"))
                ._issuerConnector_(URI.create("issuerConnector"))
                ._securityToken_(new DynamicAttributeTokenBuilder()
                        ._tokenFormat_(TokenFormat.JWT)
                        ._tokenValue_("token")
                        .build())
                ._requestedElement_(requestedElement)
                .build();

        Map<String, Object> specsMap = new HashMap<>();
        specsMap.put(QuerySpec.OFFSET, rangeFrom);
        specsMap.put(QuerySpec.LIMIT, rangeFrom + rangeTo);
        specsMap.put(QuerySpec.FILTER_EXPRESSION, List.of(Map.of("operandLeft", PROPERTY, "operator", EQUALS_SIGN, "operandRight", VALUE)));
        message.setProperty(QuerySpec.QUERY_SPEC, specsMap);
        return message;
    }

}
