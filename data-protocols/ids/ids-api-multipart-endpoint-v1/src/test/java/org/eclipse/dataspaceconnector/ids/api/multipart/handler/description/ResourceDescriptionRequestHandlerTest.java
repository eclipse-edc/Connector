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

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.Resource;
import org.eclipse.dataspaceconnector.ids.api.multipart.util.MessageFactory;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockAssetIndex;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockDescriptionRequestMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockTransformerRegistry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceDescriptionRequestHandlerTest {

    private static final URI CONNECTOR_ID = URI.create("urn:connector:edc");

    private ResourceDescriptionRequestHandler resourceDescriptionRequestHandler;

    private Monitor monitor;
    private TransformerRegistry transformerRegistry;
    private DescriptionRequestMessage descriptionRequestMessage;
    private ContractOfferService contractOfferService;
    private MultipartResponseFactory multipartResponseFactory;
    private AssetIndex assetIndex;
    private Resource resource;
    private MessageFactory messageFactory;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @BeforeEach
    public void setup() throws URISyntaxException {
        monitor = mock(Monitor.class);
        resource = mock(Resource.class);
        when(resource.getId()).thenReturn(new URI("urn:resource:hello"));

        contractOfferService = mock(ContractOfferService.class);
        descriptionRequestMessage = mockDescriptionRequestMessage(resource.getId());
        assetIndex = mockAssetIndex();
        transformerRegistry = mockTransformerRegistry(IdsType.RESOURCE);

        IdentityService identityService = mock(IdentityService.class);
        MessageFactory messageFactory = new MessageFactory(CONNECTOR_ID, identityService);
        multipartResponseFactory = mock(MultipartResponseFactory.class);
        messageFactory = mock(MessageFactory.class);

        resourceDescriptionRequestHandler = new ResourceDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, contractOfferService, transformerRegistry, multipartResponseFactory, messageFactory);

        Mockito.when(messageFactory.createDescriptionResponseMessage(Mockito.any())).thenReturn(mock(DescriptionResponseMessage.class));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConstructorArgumentsNotNullable() {
        assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(null, CONNECTOR_ID, assetIndex, contractOfferService, transformerRegistry, multipartResponseFactory, messageFactory));
        assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(monitor, null, assetIndex, contractOfferService, transformerRegistry, multipartResponseFactory, messageFactory));
        assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(monitor, CONNECTOR_ID, null, contractOfferService, transformerRegistry, multipartResponseFactory, messageFactory));
        assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, null, transformerRegistry, multipartResponseFactory, messageFactory));
        assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, contractOfferService, null, multipartResponseFactory, messageFactory));
        assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, contractOfferService, transformerRegistry, null, messageFactory));
        assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, contractOfferService, transformerRegistry, multipartResponseFactory, null));
    }

    @Test
    public void testSimpleSuccessPath() {
        var verificationResult = Result.success(ClaimToken.Builder.newInstance().build());
        when(assetIndex.findById(anyString())).thenReturn(Asset.Builder.newInstance().build());
        var resourceResult = Result.success(resource);
        when(transformerRegistry.transform(isA(OfferedAsset.class), eq(Resource.class))).thenReturn(resourceResult);
        when(contractOfferService.queryContractOffers(isA(ContractOfferQuery.class))).thenReturn(Stream.empty());

        var result = resourceDescriptionRequestHandler.handle(descriptionRequestMessage, verificationResult, null);

        assertNotNull(result);
        assertNotNull(result.getHeader());
        assertEquals(resource, result.getPayload());
        verify(resource).getId();
        verify(assetIndex).findById(anyString());
        verify(transformerRegistry).transform(isA(OfferedAsset.class), eq(Resource.class));
        verify(contractOfferService).queryContractOffers(isA(ContractOfferQuery.class));
    }
}
