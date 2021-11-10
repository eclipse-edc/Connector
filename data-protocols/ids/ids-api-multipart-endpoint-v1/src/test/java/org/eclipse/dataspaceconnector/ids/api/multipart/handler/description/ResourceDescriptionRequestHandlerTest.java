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
import de.fraunhofer.iais.eis.Resource;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.container.OfferedAsset;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferQueryResponse;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockAssetIndex;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockDescriptionRequestMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockTransformerRegistry;

public class ResourceDescriptionRequestHandlerTest {

    private static final String CONNECTOR_ID = "urn:connector:edc";

    // subject
    private ResourceDescriptionRequestHandler resourceDescriptionRequestHandler;

    // mocks
    private Monitor monitor;
    private TransformerRegistry transformerRegistry;
    private DescriptionRequestMessage descriptionRequestMessage;
    private ContractOfferService contractOfferService;
    private AssetIndex assetIndex;
    private Resource resource;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeEach
    public void setup() throws URISyntaxException {
        monitor = EasyMock.createMock(Monitor.class);
        resource = EasyMock.createMock(Resource.class);
        EasyMock.expect(resource.getId()).andReturn(new URI("urn:resource:hello"));
        EasyMock.replay(monitor, resource);

        contractOfferService = EasyMock.createMock(ContractOfferService.class);
        descriptionRequestMessage = mockDescriptionRequestMessage(resource.getId());
        assetIndex = mockAssetIndex();
        transformerRegistry = mockTransformerRegistry(IdsType.RESOURCE);

        resourceDescriptionRequestHandler = new ResourceDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, contractOfferService, transformerRegistry);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConstructorArgumentsNotNullable() {
        Assertions.assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(null, CONNECTOR_ID, assetIndex, contractOfferService, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(monitor, null, assetIndex, contractOfferService, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(monitor, CONNECTOR_ID, null, contractOfferService, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, null, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new ResourceDescriptionRequestHandler(monitor, CONNECTOR_ID, assetIndex, contractOfferService, null));
    }

    @Test
    public void testSimpleSuccessPath() {
        // prepare
        VerificationResult verificationResult = EasyMock.createMock(VerificationResult.class);

        EasyMock.expect(assetIndex.findById(EasyMock.anyString())).andReturn(EasyMock.createMock(Asset.class));

        var resourceResult = (TransformResult) EasyMock.createMock(TransformResult.class);
        EasyMock.expect(resourceResult.getOutput()).andReturn(resource);
        EasyMock.expect(resourceResult.hasProblems()).andReturn(false);
        EasyMock.expect(transformerRegistry.transform(EasyMock.isA(OfferedAsset.class), EasyMock.eq(Resource.class))).andReturn(resourceResult);

        EasyMock.expect(contractOfferService.queryContractOffers(EasyMock.isA(ContractOfferQuery.class))).andReturn(new ContractOfferQueryResponse(Stream.empty()));

        EasyMock.replay(assetIndex, contractOfferService, transformerRegistry, descriptionRequestMessage, resourceResult);

        // invoke
        var result = resourceDescriptionRequestHandler.handle(descriptionRequestMessage, verificationResult, null);

        // validate
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getHeader());
        Assertions.assertEquals(resource, result.getPayload());
    }
}
